/*
 * Run Task Gradle Plugins
 * Copyright (c) 2024 Jason Penilla
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
package xyz.jpenilla.runtask.pluginsapi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import xyz.jpenilla.runtask.util.Constants
import xyz.jpenilla.runtask.util.Downloader
import xyz.jpenilla.runtask.util.HashingAlgorithm
import xyz.jpenilla.runtask.util.calculateHash
import xyz.jpenilla.runtask.util.path
import xyz.jpenilla.runtask.util.prettyPrint
import xyz.jpenilla.runtask.util.toHexString
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.jar.JarFile
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

internal abstract class PluginDownloadServiceImpl : PluginDownloadService {

  companion object {
    private val LOGGER = Logging.getLogger(PluginDownloadServiceImpl::class.java)
  }

  // The general assumption here is plugin versions are largely static. For plugins, when a new version is released it
  // tends to be a new version. We still check periodically just in case it changes, but the expectation is that will
  // be fairly rare.
  private val minimumCheckDuration = Duration.ofDays(7)

  private val mapper: JsonMapper = JsonMapper.builder()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .addModule(kotlinModule())
    .build()

  private val manifestFile: Path = parameters.cacheDirectory.file("manifest.json").path
  private var manifest: PluginsManifest = loadOrCreateManifest()

  private fun loadOrCreateManifest(): PluginsManifest {
    return if (!manifestFile.isRegularFile()) {
      PluginsManifest()
    } else {
      manifestFile.bufferedReader().use { reader -> mapper.readValue(reader) }
    }
  }

  private fun writeManifest() {
    val dir = manifestFile.parent
    if (!dir.isDirectory()) {
      dir.createDirectories()
    }
    manifestFile.bufferedWriter().use { writer ->
      mapper.writeValue(writer, manifest)
    }
  }

  @Synchronized
  override fun resolvePlugin(progressLoggerFactory: ProgressLoggerFactory, download: PluginApiDownload): Path {
    manifest = loadOrCreateManifest()

    return when (download) {
      is HangarApiDownload -> resolveHangarPlugin(progressLoggerFactory, download)
      is ModrinthApiDownload -> resolveModrinthPlugin(progressLoggerFactory, download)
      is GitHubApiDownload -> resolveGitHubPlugin(progressLoggerFactory, download)
      is UrlDownload -> resolveUrl(progressLoggerFactory, download)
    }
  }

  private val refreshDependencies: Boolean
    get() = parameters.refreshDependencies.get()
  private val offlineMode: Boolean
    get() = parameters.offlineMode.get()

  private fun resolveUrl(progressLoggerFactory: ProgressLoggerFactory, download: UrlDownload): Path {
    val cacheDir = parameters.cacheDirectory.get().asFile.toPath()
    val targetDir = cacheDir.resolve(Constants.URL_PLUGIN_DIR)
    val urlHash = download.urlHash()
    val version = manifest.urlProvider[urlHash] ?: PluginVersion(fileName = "$urlHash.jar", displayName = download.url.get())
    val targetFile = targetDir.resolve(version.fileName)
    val setter: (PluginVersion) -> Unit = { manifest.urlProvider[urlHash] = it }
    val ctx = DownloadCtx(progressLoggerFactory, "url", download.url.get(), targetDir, targetFile, version, setter)
    return download(ctx)
  }

  private fun resolveHangarPlugin(progressLoggerFactory: ProgressLoggerFactory, download: HangarApiDownload): Path {
    val platformType = parameters.platformType.get()
    val cacheDir = parameters.cacheDirectory.get().asFile.toPath()

    val apiUrl = download.url.get().trimEnd('/')
    val apiPlugin = download.plugin.get()
    val apiVersion = download.version.get()

    val provider = manifest.hangarProviders.computeIfAbsent(apiUrl) { HangarProvider() }
    val plugin = provider.computeIfAbsent(apiPlugin) { HangarPlatforms() }
    val platform = plugin.computeIfAbsent(platformType.name) { PluginVersions() }
    val version = platform[apiVersion] ?: PluginVersion(
      fileName = "$apiPlugin-${platformType.name}-$apiVersion.jar",
      displayName = "hangar:$apiPlugin:$apiVersion:${platformType.name}"
    )

    val targetDir =
      cacheDir.resolve(Constants.HANGAR_PLUGIN_DIR).resolve(apiPlugin).resolve(apiVersion)
    val targetFile = targetDir.resolve(version.fileName)
    val downloadUrl = "$apiUrl/api/v1/projects/$apiPlugin/versions/$apiVersion/$platformType/download"

    val setter: (PluginVersion) -> Unit = { platform[apiVersion] = it }
    val ctx = DownloadCtx(progressLoggerFactory, apiUrl, downloadUrl, targetDir, targetFile, version, setter)
    return download(ctx)
  }

  private fun resolveModrinthPlugin(progressLoggerFactory: ProgressLoggerFactory, download: ModrinthApiDownload): Path {
    val cacheDir = parameters.cacheDirectory.get().asFile.toPath()

    val apiVersion = download.version.get()
    val apiPlugin = download.id.get()
    val apiUrl = download.url.get()

    val provider = manifest.modrinthProviders.computeIfAbsent(download.url.get()) { ModrinthProvider() }
    val plugin = provider.computeIfAbsent(download.id.get()) { PluginVersions() }
    val jsonVersionName = "$apiVersion-json"
    val jsonVersion = plugin[jsonVersionName] ?: PluginVersion(fileName = "$apiPlugin-$apiVersion-info.json", displayName = "modrinth:$apiPlugin:$apiVersion:metadata")

    val targetDir =
      cacheDir.resolve(Constants.MODRINTH_PLUGIN_DIR).resolve(apiPlugin)
    val jsonFile = targetDir.resolve(jsonVersion.fileName)

    val versionRequestUrl = "$apiUrl/v2/project/$apiPlugin/version/$apiVersion"
    val versionJsonPath = download(
      DownloadCtx(progressLoggerFactory, apiUrl, versionRequestUrl, targetDir, jsonFile, jsonVersion, setter = { plugin[jsonVersionName] = it }, requireValidJar = false)
    )
    val versionInfo = mapper.readValue<ModrinthVersionResponse>(versionJsonPath.toFile())
    val primaryFile = versionInfo.files.find { it.primary } ?: error("Could not find primary file for $download in $versionInfo")

    val version = plugin[apiVersion] ?: PluginVersion(
      fileName = "$apiPlugin-$apiVersion.jar",
      displayName = "modrinth:$apiPlugin:$apiVersion",
      hash = Hash(primaryFile.hashes["sha512"] ?: error("Missing hash in $primaryFile"), HashingAlgorithm.SHA512.name)
    )
    val targetFile = targetDir.resolve(version.fileName)

    return download(
      DownloadCtx(progressLoggerFactory, apiUrl, primaryFile.url, targetDir, targetFile, version, setter = { plugin[apiVersion] = it })
    )
  }

  private fun resolveGitHubPlugin(progressLoggerFactory: ProgressLoggerFactory, download: GitHubApiDownload): Path {
    val cacheDir = parameters.cacheDirectory.get().asFile.toPath()

    val owner = download.owner.get()
    val repo = download.repo.get()
    val tag = download.tag.get()
    val asset = download.assetName.get()

    val ownerProvider = manifest.githubProvider.computeIfAbsent(owner) { GitHubOwner() }
    val repoProvider = ownerProvider.computeIfAbsent(repo) { GitHubRepo() }
    val tagProvider = repoProvider.computeIfAbsent(tag) { PluginVersions() }
    val version = tagProvider[asset] ?: PluginVersion(fileName = asset, displayName = "github:$owner/$repo:$tag/$asset")

    val targetDir =
      cacheDir.resolve(Constants.GITHUB_PLUGIN_DIR).resolve(owner).resolve(repo).resolve(tag)
    val targetFile = targetDir.resolve(version.fileName)
    val downloadUrl = "https://github.com/$owner/$repo/releases/download/$tag/$asset"

    val setter: (PluginVersion) -> Unit = { tagProvider[asset] = it }
    val ctx = DownloadCtx(progressLoggerFactory, "github.com", downloadUrl, targetDir, targetFile, version, setter)
    return download(ctx)
  }

  private fun download(ctx: DownloadCtx): Path {
    if (refreshDependencies) {
      return downloadFile(ctx)
    }

    return if (ctx.targetFile.isRegularFile()) {
      val now = Instant.now()
      val durationSinceCheck = Duration.between(Instant.ofEpochMilli(ctx.version.lastUpdateCheck), now)
      if (durationSinceCheck < minimumCheckDuration || offlineMode) {
        // assume if we have the file it's good
        return ctx.targetFile
      }

      downloadFile(ctx)
    } else {
      if (offlineMode) {
        // we don't have the file - and we can't download it
        error("Offline mode is enabled and could not locate a locally cached plugin of ${ctx.version.fileName}")
      }

      return downloadFile(ctx)
    }
  }

  private fun downloadFile(ctx: DownloadCtx): Path {
    val url = URI.create(ctx.downloadUrl).toURL()
    val connection = url.openConnection() as HttpURLConnection

    try {
      connection.instanceFollowRedirects = true
      connection.setRequestProperty("Accept", "application/octet-stream")
      connection.setRequestProperty("User-Agent", Constants.USER_AGENT)

      if (ctx.targetFile.isRegularFile()) {
        if (ctx.version.lastUpdateCheck > 0 && ctx.version.hash?.check(ctx.targetFile) != false) {
          // File matches what we expected
          connection.ifModifiedSince = ctx.version.lastUpdateCheck

          if (ctx.version.etag != null) {
            connection.setRequestProperty("If-None-Match", ctx.version.etag)
          }
        } else {
          // The file exists, but we have no way of verifying it (or it is invalid)
          ctx.targetFile.deleteExisting()
        }
      }

      connection.connect()

      val status = connection.responseCode
      val displayName = ctx.version.displayName ?: ctx.version.fileName
      if (status == HttpURLConnection.HTTP_NOT_MODIFIED) {
        // not modified
        ctx.setter(ctx.version.copy(lastUpdateCheck = Instant.now().toEpochMilli()))
        writeManifest()
        return ctx.targetFile
      }

      if (status in 200..299) {
        if (!ctx.targetDir.isDirectory()) {
          ctx.targetDir.createDirectories()
        }

        val opName = "${ctx.baseUrl}:${ctx.version.fileName}"
        val start = Instant.now()
        LOGGER.lifecycle("Downloading {}...", displayName)
        when (val res = Downloader(url, ctx.targetFile, displayName, opName).download(ctx.progressLoggerFactory, connection)) {
          is Downloader.Result.Success -> LOGGER.lifecycle("Done downloading {}, took {}.", displayName, Duration.between(start, Instant.now()).prettyPrint())
          is Downloader.Result.Failure -> throw IllegalStateException("Failed to download $displayName.", res.throwable)
        }

        val etagValue: String? = connection.getHeaderField("ETag")
        requireValidJarFile(ctx, displayName)
        ctx.setter(ctx.version.copy(lastUpdateCheck = Instant.now().toEpochMilli(), etag = etagValue))
        writeManifest()
        return ctx.targetFile
      }

      throw IllegalStateException("Failed to download $displayName, status code: $status")
    } finally {
      connection.disconnect()
    }
  }

  private fun requireValidJarFile(ctx: DownloadCtx, displayName: String) {
    if (!ctx.requireValidJar) {
      return
    }

    val invalidPath = ctx.targetFile.resolveSibling(ctx.targetFile.fileName.toString() + ".invalid-not-jar")

    try {
      JarFile(ctx.targetFile.toFile()).use {}
    } catch (thr: Throwable) {
      // Leave behind invalid jar (for debugging purposes), but not at destination
      // path, so it won't be used again later in the case it's from a provider
      // that doesn't provide hashes
      try {
        Files.move(ctx.targetFile, invalidPath, StandardCopyOption.REPLACE_EXISTING)
      } catch (e: IOException) {
        thr.addSuppressed(e)
        try {
          Files.deleteIfExists(ctx.targetFile)
        } catch (e: IOException) {
          thr.addSuppressed(e)
        }
      }
      throw IllegalStateException("Downloaded file for $displayName is not a valid jar file.", thr)
    }

    // Delete any left behind invalid jars if we somehow get a valid jar
    Files.deleteIfExists(invalidPath)
  }

  private data class DownloadCtx(
    val progressLoggerFactory: ProgressLoggerFactory,
    val baseUrl: String,
    val downloadUrl: String,
    val targetDir: Path,
    val targetFile: Path,
    val version: PluginVersion,
    val setter: (PluginVersion) -> Unit,
    val requireValidJar: Boolean = true,
  )
}

// type aliases used to just prevent this from being a horribly nested type to look at
private data class PluginsManifest(
  val hangarProviders: MutableMap<String, HangarProvider> = HashMap(),
  val modrinthProviders: MutableMap<String, ModrinthProvider> = HashMap(),
  val githubProvider: GitHubProvider = GitHubProvider(),
  val urlProvider: PluginVersions = PluginVersions()
)

// hangar types:
private typealias HangarProvider = MutableMap<String, HangarPlatforms>

private typealias HangarPlatforms = MutableMap<String, PluginVersions>

private fun HangarProvider(): HangarProvider = HashMap()

private fun HangarPlatforms(): HangarPlatforms = HashMap()

// modrinth types:
private typealias ModrinthProvider = MutableMap<String, PluginVersions>

private fun ModrinthProvider(): ModrinthProvider = HashMap()

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ModrinthVersionResponse(
  val files: List<FileData>
) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  data class FileData(
    val url: String,
    val primary: Boolean,
    val hashes: Map<String, String>
  )
}

// github types:
private typealias GitHubProvider = MutableMap<String, GitHubOwner>

private fun GitHubProvider(): GitHubProvider = HashMap()
private typealias GitHubOwner = MutableMap<String, GitHubRepo>

private fun GitHubOwner(): GitHubOwner = HashMap()
private typealias GitHubRepo = MutableMap<String, PluginVersions>

private fun GitHubRepo(): GitHubRepo = HashMap()

// general types:
private typealias PluginVersions = MutableMap<String, PluginVersion>

private fun PluginVersions(): PluginVersions = HashMap()
private data class PluginVersion(
  val lastUpdateCheck: Long = 0L,
  val etag: String? = null,
  val hash: Hash? = null,
  val fileName: String,
  val displayName: String? = null
)

private data class Hash(
  val hash: String,
  val type: String
) {
  fun type() = HashingAlgorithm.valueOf(type.uppercase(Locale.ENGLISH))

  fun check(file: Path): Boolean = toHexString(file.calculateHash(type())) == hash
}
