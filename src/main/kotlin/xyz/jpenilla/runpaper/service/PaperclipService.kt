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
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import xyz.jpenilla.runpaper.paperapi.DownloadsAPI
import xyz.jpenilla.runpaper.paperapi.Projects
import xyz.jpenilla.runpaper.task.RunServerTask
import xyz.jpenilla.runpaper.util.FileHashing
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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

  private fun paperclipsFor(minecraftVersion: String): Provider<Directory> =
    this.paperclips().map { it.dir(minecraftVersion) }

  private fun paperclips(): Provider<Directory> =
    this.parameters.cacheDirectory.dir("paperclips")

  fun resolvePaperclip(minecraftVersion: String, paperBuild: RunServerTask.PaperBuild): File {
    this.versions = this.loadOrCreateVersions()
    val build = this.resolveBuildNumber(minecraftVersion, paperBuild)

    val version = this.versions.versions.computeIfAbsent(minecraftVersion) { Version(it) }

    val possible = version.knownJars[build]
    if (possible != null && !this.parameters.refreshDependencies.get()) {
      // We already have this Paperclip!
      LOGGER.lifecycle("Located Paper {} build {} in local cache.", minecraftVersion, build)

      // Verify hash is still correct
      val localPaperclip = this.paperclipsFor(minecraftVersion).get().file(possible.fileName).asFile
      val localBuildHash = FileHashing.sha256(localPaperclip)
      if (localBuildHash == possible.sha256) {
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

    FileOutputStream(tempFile.toFile()).use { fileOutputStream ->
      Channels.newChannel(downloadURL.openStream()).use { downloadChannel ->
        fileOutputStream.channel.transferFrom(downloadChannel, 0, Long.MAX_VALUE)
      }
    }

    LOGGER.lifecycle("Done downloading Paper.")

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

    version.knownJars[build] = PaperclipJar(build, fileName, download.sha256)
    this.writeVersions()

    return destination
  }

  private fun resolveBuildNumber(minecraftVersion: String, paperBuild: RunServerTask.PaperBuild): Int {
    if (paperBuild != RunServerTask.PaperBuild.LATEST) {
      return paperBuild.buildNumber
    }
    if (this.parameters.offlineMode.get()) {
      LOGGER.lifecycle("Offline mode enabled, attempting to use latest local build of Paper for Minecraft {}.", minecraftVersion)
      return this.resolveLatestLocalBuild(minecraftVersion)
    }
    return this.resolveLatestRemoteBuild(minecraftVersion)
  }

  private fun resolveLatestLocalBuild(minecraftVersion: String): Int {
    val version = this.versions.versions[minecraftVersion] ?: this.unknownMinecraftVersion(minecraftVersion)
    return version.knownJars.keys.maxOrNull() ?: this.unknownMinecraftVersion(minecraftVersion)
  }

  private fun resolveLatestRemoteBuild(minecraftVersion: String): Int =
    try {
      LOGGER.lifecycle("Fetching Paper builds for Minecraft {}...", minecraftVersion)
      this.api.version(Projects.PAPER, minecraftVersion).builds.last().apply {
        LOGGER.lifecycle("Latest build for {} is {}.", minecraftVersion, this)
      }
    } catch (ex: Exception) {
      LOGGER.lifecycle("Failed to check for latest release, attempting to use latest local build.")
      this.resolveLatestLocalBuild(minecraftVersion)
    }

  private fun logExpectedActual(expected: String, actual: String) {
    LOGGER.lifecycle(" > Expected: {}", expected)
    LOGGER.lifecycle(" > Actual: {}", actual)
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

  data class PaperclipJar(val buildNumber: Int, val fileName: String, val sha256: String)

  data class Versions(val versions: MutableMap<String, Version> = HashMap())

  data class Version(
    val name: String,
    val knownJars: MutableMap<Int, PaperclipJar> = HashMap()
  )
}
