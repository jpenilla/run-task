/*
 * Run Task Gradle Plugins
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
package xyz.jpenilla.runtask.service

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
import xyz.jpenilla.runtask.paperapi.DownloadsAPI
import xyz.jpenilla.runtask.util.Constants
import xyz.jpenilla.runtask.util.Downloader
import xyz.jpenilla.runtask.util.InvalidDurationException
import xyz.jpenilla.runtask.util.LoggingDownloadListener
import xyz.jpenilla.runtask.util.ProgressLoggerUtil
import xyz.jpenilla.runtask.util.parseDuration
import xyz.jpenilla.runtask.util.path
import xyz.jpenilla.runtask.util.prettyPrint
import xyz.jpenilla.runtask.util.sha256
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

internal abstract class DownloadsAPIServiceImpl : BuildService<DownloadsAPIServiceImpl.Parameters>, AutoCloseable, DownloadsAPIService {
  interface Parameters : BuildServiceParameters {
    val downloadsEndpoint: Property<String>
    val downloadProject: Property<String>
    val downloadProjectDisplayName: Property<String>
    val cacheDirectory: DirectoryProperty
    val refreshDependencies: Property<Boolean>
    val offlineMode: Property<Boolean>
  }

  companion object {
    private val LOGGER: Logger = Logging.getLogger(DownloadsAPIServiceImpl::class.java)
  }

  private val api: DownloadsAPI = DownloadsAPI(parameters.downloadsEndpoint.get())
  private val mapper: JsonMapper = JsonMapper.builder()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .addModule(kotlinModule())
    .build()
  private val versionsFile: Path = parameters.cacheDirectory.file("versions.json").path

  private var versions: Versions = loadOrCreateVersions()

  init {
    cleanLocalCache()
  }

  /**
   * Cleans the local cache. We only keep a certain amount of builds for each version.
   * Builds fetched by version number and not using [DownloadsAPIService.Build.Latest]
   * will not be cleaned up, the cache cleaning tasks must be used to clear these.
   */
  private fun cleanLocalCache() {
    // how many builds to cache for each version
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

        val oldJar = jarsFor(versionName).resolve(removed.fileName)
        try {
          oldJar.deleteIfExists()
        } catch (ex: IOException) {
          LOGGER.warn("Failed to delete jar at {}", oldJar.absolutePathString(), ex)
        }
        writeVersions()
      }
    }
  }

  private fun jarsFor(version: String): Path =
    jars.resolve(version)

  private val displayName: String
    get() = parameters.downloadProjectDisplayName.get()

  private val jars: Path
    get() = parameters.cacheDirectory.path.resolve("jars")

  @Synchronized
  override fun resolveBuild(
    project: Project,
    version: String,
    build: DownloadsAPIService.Build
  ): Path {
    versions = loadOrCreateVersions()

    val versionData = versions.versions.computeIfAbsent(version) { Version(it) }
    val buildNumber = resolveBuildNumber(project, versionData, build)

    val possible = versionData.knownJars[buildNumber]
    if (possible != null && !parameters.refreshDependencies.get()) {
      // We already have this jar!
      LOGGER.lifecycle("Located {} {} build {} in local cache.", displayName, version, buildNumber)

      // Verify hash is still correct
      val localJar = jarsFor(version).resolve(possible.fileName)
      val localBuildHash = localJar.sha256()
      if (localBuildHash == possible.sha256) {
        if (build is DownloadsAPIService.Build.Specific) {
          versionData.knownJars[buildNumber] = possible.copy(keep = true)
          writeVersions()
        }
        // Hash is good, return
        return localJar
      }
      versionData.knownJars.remove(buildNumber)
      writeVersions()
      localJar.deleteIfExists()
      LOGGER.lifecycle("Invalid SHA256 hash for locally cached {} {} build {}, invalidating and attempting to re-download.", displayName, version, buildNumber)
      logExpectedActual(possible.sha256, localBuildHash)
    }

    // Need to fetch new jar!
    if (parameters.offlineMode.get()) {
      error("Offline mode is enabled and could not locate a locally cached build of $displayName $version.")
    }
    LOGGER.lifecycle("Downloading {} {} build {}...", displayName, version, buildNumber)
    val buildResponse = api.build(parameters.downloadProject.get(), version, buildNumber)
    val download = buildResponse.downloads["application"] ?: error("Could not find download.")
    val downloadLink = api.downloadURL(parameters.downloadProject.get(), version, buildNumber, download)
    val downloadURL = URL(downloadLink)

    val tempFile = createTempDirectory("downloads-api-service-impl")
      .resolve("${parameters.downloadProject.get()}-$version-$buildNumber-${System.currentTimeMillis()}.jar.tmp")

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

    val jarsDir = jarsFor(version)
    jarsDir.createDirectories()
    val fileName = "$buildNumber.jar"
    val destination = jarsDir.resolve(fileName)

    tempFile.moveTo(destination, StandardCopyOption.REPLACE_EXISTING)

    versionData.knownJars[buildNumber] = JarInfo(
      buildNumber,
      fileName,
      download.sha256,
      // If the build was specifically requested, (as opposed to resolved as latest) mark the jar for keeping
      build is DownloadsAPIService.Build.Specific
    )
    writeVersions()

    return destination
  }

  private fun resolveBuildNumber(
    project: Project,
    version: Version,
    build: DownloadsAPIService.Build
  ): Int {
    if (build is DownloadsAPIService.Build.Specific) {
      return build.buildNumber
    }

    if (parameters.offlineMode.get()) {
      LOGGER.lifecycle("Offline mode enabled, attempting to use latest local build of {} {}.", displayName, version)
      return resolveLatestLocalBuild(version)
    }

    if (!parameters.refreshDependencies.get()) {
      val checkFrequency = updateCheckFrequency(project)
      val timeSinceLastCheck = System.currentTimeMillis() - version.lastUpdateCheck
      if (timeSinceLastCheck <= checkFrequency.toMillis()) {
        return resolveLatestLocalBuild(version)
      }
    }

    return resolveLatestRemoteBuild(version)
  }

  private fun resolveLatestLocalBuild(version: Version): Int {
    return version.knownJars.keys.maxOrNull()
      ?: unknownVersion(version.name)
  }

  private fun resolveLatestRemoteBuild(version: Version): Int = try {
    LOGGER.lifecycle("Fetching {} builds for version {}...", displayName, version.name)
    api.version(parameters.downloadProject.get(), version.name).builds.last().apply {
      LOGGER.lifecycle("Latest build for {} is {}.", version.name, this)
      versions.versions[version.name] = version.copy(lastUpdateCheck = System.currentTimeMillis())
      writeVersions()
    }
  } catch (ex: Exception) {
    LOGGER.lifecycle("Failed to check for latest release, attempting to use latest local build.")
    resolveLatestLocalBuild(version)
  }

  private fun createDownloadListener(project: Project): Downloader.ProgressListener {
    // ProgressLogger is internal Gradle API and can technically be changed,
    // (although it hasn't since 3.x) so we access it using reflection, and
    // fallback to using LOGGER if it fails
    val progressLogger = ProgressLoggerUtil.createProgressLogger(project, "${parameters.downloadsEndpoint}:${parameters.downloadProject}")
    return if (progressLogger != null) {
      LoggingDownloadListener(
        progressLogger,
        { state, message -> state.start("Downloading $displayName", message) },
        { state, message -> state.progress(message) },
        { state -> state.completed() },
        "Downloading $displayName: ",
        10L
      )
    } else {
      LoggingDownloadListener(
        LOGGER,
        logger = { state, message -> state.lifecycle(message) },
        prefix = "Downloading $displayName: ",
        updateRateMs = 1000L
      )
    }
  }

  private fun logExpectedActual(expected: String, actual: String) {
    LOGGER.lifecycle(" > Expected: {}", expected)
    LOGGER.lifecycle(" > Actual: {}", actual)
  }

  private fun updateCheckFrequency(project: Project): Duration {
    var prop = project.findProperty(Constants.Properties.UPDATE_CHECK_FREQUENCY)
    if (prop == null) {
      prop = project.findProperty(Constants.Properties.UPDATE_CHECK_FREQUENCY_LEGACY)
      if (prop != null) {
        LOGGER.warn(
          "Use of legacy '{}' property detected. Please replace with '{}'.",
          Constants.Properties.UPDATE_CHECK_FREQUENCY_LEGACY,
          Constants.Properties.UPDATE_CHECK_FREQUENCY
        )
      }
    }
    if (prop == null) {
      return Duration.ofHours(1) // default to 1 hour if unset
    }
    try {
      return parseDuration(prop as String)
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

  private fun unknownVersion(version: String): Nothing =
    error("Unknown $displayName Version: $version")

  data class JarInfo(
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
    val knownJars: MutableMap<Int, JarInfo> = HashMap(),
  )
}
