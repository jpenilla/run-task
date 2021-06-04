/*
 * Run Paper Gradle Plugin
 * Copyright (c) 2021 Jason Penilla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jpenilla.runpaper.service

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import xyz.jpenilla.runpaper.Constants
import xyz.jpenilla.runpaper.paperapi.DownloadsAPI
import xyz.jpenilla.runpaper.paperapi.Projects
import xyz.jpenilla.runpaper.task.RunServerTask
import xyz.jpenilla.runpaper.util.Downloader
import xyz.jpenilla.runpaper.util.DurationParser
import xyz.jpenilla.runpaper.util.FileHashing
import xyz.jpenilla.runpaper.util.LoggingDownloadListener
import xyz.jpenilla.runpaper.util.ProgressLoggerUtil
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration

internal abstract class PaperclipService : BuildService<PaperclipService.Parameters>, AutoCloseable {
  interface Parameters : BuildServiceParameters {
    val cacheDirectory: DirectoryProperty
    val refreshDependencies: Property<Boolean>
    val offlineMode: Property<Boolean>
  }

  companion object {
    private val LOGGER: Logger = Logging.getLogger(PaperclipService::class.java)
  }

  private val api: DownloadsAPI = DownloadsAPI()
  private val mapper: JsonMapper = JsonMapper.builder()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .addModule(kotlinModule())
    .build()
  private val versionsFile: Provider<RegularFile> = this.parameters.cacheDirectory.file("versions.json")

  private var versions = this.loadOrCreateVersions()

  init {
    this.cleanLocalCache()
  }

  /**
   * Cleans the local Paperclip cache. We only keep a certain amount of Paperclips
   * for each Minecraft version. Paperclips fetched by version number and not using
   * [RunServerTask.PaperBuild.Latest] will not be cleaned up, the `cleanPaperclipCache`
   * task must be used to fully clear the cache for this.
   */
  private fun cleanLocalCache() {
    // how many Paperclips to cache for each Minecraft version
    val perVersionCacheSize = 5

    for ((versionName, version) in this.versions.versions) {
      val jars = version.knownJars.filter { !it.value.keep }.toMutableMap()
      if (jars.isEmpty()) {
        continue
      }
      while (jars.size > perVersionCacheSize) {
        val oldestBuild = jars.keys.minOrNull() ?: error("Could not determine oldest build.")
        val removed = jars.remove(oldestBuild) ?: error("Build does not exist?")
        version.knownJars.remove(oldestBuild)

        val oldPaperclipFile = this.paperclipsFor(versionName).get().file(removed.fileName).asFile
        try {
          oldPaperclipFile.delete()
        } catch (ex: IOException) {
          LOGGER.warn("Failed to delete Paperclip at {}", oldPaperclipFile.path, ex)
        }
        this.writeVersions()
      }
    }
  }

  private fun paperclipsFor(minecraftVersion: String): Provider<Directory> =
    this.paperclips().map { it.dir(minecraftVersion) }

  private fun paperclips(): Provider<Directory> =
    this.parameters.cacheDirectory.dir("paperclips")

  fun resolvePaperclip(
    project: Project,
    minecraftVersion: String,
    paperBuild: RunServerTask.PaperBuild
  ): File {
    this.versions = this.loadOrCreateVersions()

    val version = this.versions.versions.computeIfAbsent(minecraftVersion) { Version(it) }
    val build = this.resolveBuildNumber(project, version, paperBuild)

    val possible = version.knownJars[build]
    if (possible != null && !this.parameters.refreshDependencies.get()) {
      // We already have this Paperclip!
      LOGGER.lifecycle("Located Paper {} build {} in local cache.", minecraftVersion, build)

      // Verify hash is still correct
      val localPaperclip = this.paperclipsFor(minecraftVersion).get().file(possible.fileName).asFile
      val localBuildHash = FileHashing.sha256(localPaperclip)
      if (localBuildHash == possible.sha256) {
        if (paperBuild is RunServerTask.PaperBuild.Specific) {
          version.knownJars[build] = possible.copy(keep = true)
          this.writeVersions()
        }
        // Hash is good, return
        return localPaperclip
      }
      version.knownJars.remove(build)
      this.writeVersions()
      localPaperclip.delete()
      LOGGER.lifecycle("Invalid SHA256 hash for locally cached Paper {} build {}, invalidating and attempting to re-download.", minecraftVersion, build)
      this.logExpectedActual(possible.sha256, localBuildHash)
    }

    // Need to fetch new Paperclip!
    if (this.parameters.offlineMode.get()) {
      error("Offline mode is enabled and Run Paper could not locate a locally cached build.")
    }
    LOGGER.lifecycle("Downloading Paper {} build {}...", minecraftVersion, build)
    val buildResponse = this.api.build(Projects.PAPER, minecraftVersion, build)
    val download = buildResponse.downloads.values.first()
    val downloadLink = this.api.downloadURL(Projects.PAPER, minecraftVersion, build, download)
    val downloadURL = URL(downloadLink)

    val tempFile = Files.createTempDirectory("runpaper")
      .resolve("paperclip-$minecraftVersion-$build-${System.currentTimeMillis()}.jar.tmp")

    val downloadResult = Downloader(downloadURL, tempFile)
      .download(this.createDownloadListener(project))

    when (downloadResult) {
      is Downloader.Result.Success -> LOGGER.lifecycle("Done downloading Paper.")
      is Downloader.Result.Failure -> throw IllegalStateException("Failed to download Paper.", downloadResult.throwable)
    }

    // Verify SHA256 hash of downloaded jar
    val downloadedFileHash = FileHashing.sha256(tempFile.toFile())
    if (downloadedFileHash != download.sha256) {
      Files.delete(tempFile)
      LOGGER.lifecycle("Invalid SHA256 hash for downloaded file: '{}', deleting.", download.name)
      this.logExpectedActual(download.sha256, downloadedFileHash)
      error("Failed to verify SHA256 hash of downloaded file.")
    }
    LOGGER.lifecycle("Verified SHA256 hash of downloaded jar.")

    val paperclipsDir = this.paperclipsFor(minecraftVersion).get()
    paperclipsDir.asFile.mkdirs()
    val fileName = "paperclip-$minecraftVersion-$build.jar"
    val destination = paperclipsDir.file(fileName).asFile

    Files.move(tempFile, destination.toPath(), StandardCopyOption.REPLACE_EXISTING)

    version.knownJars[build] = PaperclipJar(
      build,
      fileName,
      download.sha256,
      // If the build was specifically requested, (as opposed to resolved as latest) mark the jar for keeping
      paperBuild is RunServerTask.PaperBuild.Specific
    )
    this.writeVersions()

    return destination
  }

  private fun resolveBuildNumber(
    project: Project,
    minecraftVersion: Version,
    paperBuild: RunServerTask.PaperBuild
  ): Int {
    if (paperBuild is RunServerTask.PaperBuild.Specific) {
      return paperBuild.buildNumber
    }

    if (this.parameters.offlineMode.get()) {
      LOGGER.lifecycle("Offline mode enabled, attempting to use latest local build of Paper for Minecraft {}.", minecraftVersion)
      return this.resolveLatestLocalBuild(minecraftVersion)
    }

    if (!this.parameters.refreshDependencies.get()) {
      val checkFrequency = this.updateCheckFrequency(project)
      val timeSinceLastCheck = System.currentTimeMillis() - minecraftVersion.lastUpdateCheck
      if (timeSinceLastCheck <= checkFrequency.toMillis()) {
        return this.resolveLatestLocalBuild(minecraftVersion)
      }
    }

    return this.resolveLatestRemoteBuild(minecraftVersion)
  }

  private fun resolveLatestLocalBuild(minecraftVersion: Version): Int {
    return minecraftVersion.knownJars.keys.maxOrNull()
      ?: this.unknownMinecraftVersion(minecraftVersion.name)
  }

  private fun resolveLatestRemoteBuild(minecraftVersion: Version): Int = try {
    LOGGER.lifecycle("Fetching Paper builds for Minecraft {}...", minecraftVersion.name)
    this.api.version(Projects.PAPER, minecraftVersion.name).builds.last().apply {
      LOGGER.lifecycle("Latest build for {} is {}.", minecraftVersion.name, this)
      this@PaperclipService.versions.versions[minecraftVersion.name] = minecraftVersion.copy(lastUpdateCheck = System.currentTimeMillis())
      this@PaperclipService.writeVersions()
    }
  } catch (ex: Exception) {
    LOGGER.lifecycle("Failed to check for latest release, attempting to use latest local build.")
    this.resolveLatestLocalBuild(minecraftVersion)
  }

  private fun createDownloadListener(project: Project): Downloader.ProgressListener {
    // ProgressLogger is internal Gradle API and can technically be changed,
    // (although it hasn't since 3.x) so we access it using reflection, and
    // fallback to using LOGGER if it fails
    val progressLogger = ProgressLoggerUtil.createProgressLogger(project, Constants.RUN_PAPER)
    return if (progressLogger != null) {
      LoggingDownloadListener(
        progressLogger,
        { state, message -> state.start("Downloading Paper", message) },
        { state, message -> state.progress(message) },
        { state -> state.completed() },
        "Downloading Paperclip: ",
        10L
      )
    } else {
      LoggingDownloadListener(
        LOGGER,
        logger = { state, message -> state.lifecycle(message) },
        prefix = "Downloading Paperclip: ",
        updateRateMs = 1000L
      )
    }
  }

  private fun logExpectedActual(expected: String, actual: String) {
    LOGGER.lifecycle(" > Expected: {}", expected)
    LOGGER.lifecycle(" > Actual: {}", actual)
  }

  private fun updateCheckFrequency(project: Project): Duration =
    project.findProperty(Constants.Properties.UPDATE_CHECK_FREQUENCY).run {
      if (this == null) {
        return@run Duration.ofHours(1) // default to 1 hour if unset
      }
      try {
        DurationParser.parse(this as String)
      } catch (ex: DurationParser.InvalidDurationException) {
        throw IllegalArgumentException("Unable to parse value for property '${Constants.Properties.UPDATE_CHECK_FREQUENCY}'.\n${ex.message}", ex)
      }
    }

  override fun close() {
  }

  private fun loadOrCreateVersions(): Versions {
    val versions = this.versionsFile.get().asFile
    return if (!versions.exists()) {
      Versions()
    } else {
      this.mapper.readValue(versions)
    }
  }

  private fun writeVersions() {
    this.parameters.cacheDirectory.get().asFile.mkdirs()
    this.mapper.writeValue(this.versionsFile.get().asFile, this.versions)
  }

  private fun unknownMinecraftVersion(minecraftVersion: String): Nothing =
    error("Unknown Minecraft Version: $minecraftVersion")

  data class PaperclipJar(
    val buildNumber: Int,
    val fileName: String,
    val sha256: String,
    val keep: Boolean = false
  )

  data class Versions(
    val versions: MutableMap<String, Version> = HashMap()
  )

  data class Version(
    val name: String,
    val lastUpdateCheck: Long = 0L,
    val knownJars: MutableMap<Int, PaperclipJar> = HashMap(),
  )
}
