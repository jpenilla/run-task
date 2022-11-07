/*
 * Run Paper Gradle Plugin
 * Copyright (c) 2021-2022 Jason Penilla
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
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import xyz.jpenilla.runpaper.Constants
import xyz.jpenilla.runpaper.paperapi.DownloadsAPI
import xyz.jpenilla.runpaper.util.Downloader
import xyz.jpenilla.runpaper.util.InvalidDurationException
import xyz.jpenilla.runpaper.util.LoggingDownloadListener
import xyz.jpenilla.runpaper.util.ProgressLoggerUtil
import xyz.jpenilla.runpaper.util.parseDuration
import xyz.jpenilla.runpaper.util.path
import xyz.jpenilla.runpaper.util.prettyPrint
import xyz.jpenilla.runpaper.util.sha256
import java.io.IOException
import java.net.URL
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import kotlin.io.path.absolutePathString
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile
import kotlin.io.path.moveTo

internal abstract class PaperclipServiceImpl : BuildService<PaperclipServiceImpl.Parameters>, AutoCloseable, PaperclipService {
  interface Parameters : BuildServiceParameters {
    val downloadsEndpoint: Property<String>
    val downloadProject: Property<String>
    val downloadProjectDisplayName: Property<String>
    val cacheDirectory: DirectoryProperty
    val refreshDependencies: Property<Boolean>
    val offlineMode: Property<Boolean>
  }

  companion object {
    private val LOGGER: Logger = Logging.getLogger(PaperclipServiceImpl::class.java)
  }

  private val api: DownloadsAPI = DownloadsAPI(parameters.downloadsEndpoint.get())
  private val mapper: JsonMapper = JsonMapper.builder()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .addModule(kotlinModule())
    .build()
  private val versionsFile: Path = parameters.cacheDirectory.file("versions.json").path

  private var versions = loadOrCreateVersions()

  init {
    cleanLocalCache()
  }

  /**
   * Cleans the local Paperclip cache. We only keep a certain amount of Paperclips
   * for each Minecraft version. Paperclips fetched by version number and not using
   * [PaperclipService.Build.Latest] will not be cleaned up, the `cleanPaperclipCache`
   * task must be used to fully clear the cache for this.
   */
  private fun cleanLocalCache() {
    // how many Paperclips to cache for each Minecraft version
    val perVersionCacheSize = 5

    for ((versionName, version) in versions.versions) {
      val jars = version.knownJars.filterNot { it.value.keep }.toMutableMap()
      if (jars.isEmpty()) {
        continue
      }
      while (jars.size > perVersionCacheSize) {
        val oldestBuild = jars.keys.minOrNull() ?: error("Could not determine oldest build.")
        val removed = jars.remove(oldestBuild) ?: error("Build does not exist?")
        version.knownJars.remove(oldestBuild)

        val oldPaperclipFile = paperclipsFor(versionName).resolve(removed.fileName)
        try {
          oldPaperclipFile.deleteIfExists()
        } catch (ex: IOException) {
          LOGGER.warn("Failed to delete Paperclip at {}", oldPaperclipFile.absolutePathString(), ex)
        }
        writeVersions()
      }
    }
  }

  private fun paperclipsFor(minecraftVersion: String): Path =
    paperclips.resolve(minecraftVersion)

  private val displayName: String
    get() = parameters.downloadProjectDisplayName.get()

  private val paperclips: Path
    get() = parameters.cacheDirectory.path.resolve("paperclips")

  @Synchronized
  override fun resolvePaperclip(
    project: Project,
    minecraftVersion: String,
    paperBuild: PaperclipService.Build
  ): Path {
    versions = loadOrCreateVersions()

    val version = versions.versions.computeIfAbsent(minecraftVersion) { Version(it) }
    val build = resolveBuildNumber(project, version, paperBuild)

    val possible = version.knownJars[build]
    if (possible != null && !parameters.refreshDependencies.get()) {
      // We already have this Paperclip!
      LOGGER.lifecycle("Located {} {} build {} in local cache.", displayName, minecraftVersion, build)

      // Verify hash is still correct
      val localPaperclip = paperclipsFor(minecraftVersion).resolve(possible.fileName)
      val localBuildHash = localPaperclip.sha256()
      if (localBuildHash == possible.sha256) {
        if (paperBuild is PaperclipService.Build.Specific) {
          version.knownJars[build] = possible.copy(keep = true)
          writeVersions()
        }
        // Hash is good, return
        return localPaperclip
      }
      version.knownJars.remove(build)
      writeVersions()
      localPaperclip.deleteIfExists()
      LOGGER.lifecycle("Invalid SHA256 hash for locally cached {} {} build {}, invalidating and attempting to re-download.", displayName, minecraftVersion, build)
      logExpectedActual(possible.sha256, localBuildHash)
    }

    // Need to fetch new Paperclip!
    if (parameters.offlineMode.get()) {
      error("Offline mode is enabled and Run Paper could not locate a locally cached build.")
    }
    LOGGER.lifecycle("Downloading {} {} build {}...", displayName, minecraftVersion, build)
    val buildResponse = api.build(parameters.downloadProject.get(), minecraftVersion, build)
    val download = buildResponse.downloads["application"] ?: error("Could not find download.")
    val downloadLink = api.downloadURL(parameters.downloadProject.get(), minecraftVersion, build, download)
    val downloadURL = URL(downloadLink)

    val tempFile = createTempDirectory("runpaper")
      .resolve("paperclip-$minecraftVersion-$build-${System.currentTimeMillis()}.jar.tmp")

    val start = System.currentTimeMillis()
    val downloadResult = Downloader(downloadURL, tempFile)
      .download(createDownloadListener(project))

    when (downloadResult) {
      is Downloader.Result.Success -> LOGGER.lifecycle("Done downloading {}, took {}.", displayName, Duration.ofMillis(System.currentTimeMillis() - start).prettyPrint())
      is Downloader.Result.Failure -> throw IllegalStateException("Failed to download $displayName.", downloadResult.throwable)
    }

    // Verify SHA256 hash of downloaded jar
    val downloadedFileHash = tempFile.sha256()
    if (downloadedFileHash != download.sha256) {
      tempFile.deleteIfExists()
      LOGGER.lifecycle("Invalid SHA256 hash for downloaded file: '{}', deleting.", download.name)
      logExpectedActual(download.sha256, downloadedFileHash)
      error("Failed to verify SHA256 hash of downloaded file.")
    }
    LOGGER.lifecycle("Verified SHA256 hash of downloaded jar.")

    val paperclipsDir = paperclipsFor(minecraftVersion)
    paperclipsDir.createDirectories()
    val fileName = "paperclip-$minecraftVersion-$build.jar"
    val destination = paperclipsDir.resolve(fileName)

    tempFile.moveTo(destination, StandardCopyOption.REPLACE_EXISTING)

    version.knownJars[build] = PaperclipJar(
      build,
      fileName,
      download.sha256,
      // If the build was specifically requested, (as opposed to resolved as latest) mark the jar for keeping
      paperBuild is PaperclipService.Build.Specific
    )
    writeVersions()

    return destination
  }

  private fun resolveBuildNumber(
    project: Project,
    minecraftVersion: Version,
    paperBuild: PaperclipService.Build
  ): Int {
    if (paperBuild is PaperclipService.Build.Specific) {
      return paperBuild.buildNumber
    }

    if (parameters.offlineMode.get()) {
      LOGGER.lifecycle("Offline mode enabled, attempting to use latest local build of {} for Minecraft {}.", displayName, minecraftVersion)
      return resolveLatestLocalBuild(minecraftVersion)
    }

    if (!parameters.refreshDependencies.get()) {
      val checkFrequency = updateCheckFrequency(project)
      val timeSinceLastCheck = System.currentTimeMillis() - minecraftVersion.lastUpdateCheck
      if (timeSinceLastCheck <= checkFrequency.toMillis()) {
        return resolveLatestLocalBuild(minecraftVersion)
      }
    }

    return resolveLatestRemoteBuild(minecraftVersion)
  }

  private fun resolveLatestLocalBuild(minecraftVersion: Version): Int {
    return minecraftVersion.knownJars.keys.maxOrNull()
      ?: unknownMinecraftVersion(minecraftVersion.name)
  }

  private fun resolveLatestRemoteBuild(minecraftVersion: Version): Int = try {
    LOGGER.lifecycle("Fetching {} builds for Minecraft {}...", displayName, minecraftVersion.name)
    api.version(parameters.downloadProject.get(), minecraftVersion.name).builds.last().apply {
      LOGGER.lifecycle("Latest build for {} is {}.", minecraftVersion.name, this)
      versions.versions[minecraftVersion.name] = minecraftVersion.copy(lastUpdateCheck = System.currentTimeMillis())
      writeVersions()
    }
  } catch (ex: Exception) {
    LOGGER.lifecycle("Failed to check for latest release, attempting to use latest local build.")
    resolveLatestLocalBuild(minecraftVersion)
  }

  private fun createDownloadListener(project: Project): Downloader.ProgressListener {
    // ProgressLogger is internal Gradle API and can technically be changed,
    // (although it hasn't since 3.x) so we access it using reflection, and
    // fallback to using LOGGER if it fails
    val progressLogger = ProgressLoggerUtil.createProgressLogger(project, Constants.RUN_PAPER)
    return if (progressLogger != null) {
      LoggingDownloadListener(
        progressLogger,
        { state, message -> state.start("Downloading $displayName", message) },
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
        parseDuration(this as String)
      } catch (ex: InvalidDurationException) {
        throw InvalidUserDataException("Unable to parse value for property '${Constants.Properties.UPDATE_CHECK_FREQUENCY}'.\n${ex.message}", ex)
      }
    }

  override fun close() {
  }

  private fun loadOrCreateVersions(): Versions {
    return if (!versionsFile.isRegularFile()) {
      Versions()
    } else {
      versionsFile.bufferedReader().use { reader -> mapper.readValue(reader) }
    }
  }

  private fun writeVersions() {
    versionsFile.parent.createDirectories()
    versionsFile.bufferedWriter().use { writer -> mapper.writeValue(writer, versions) }
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
