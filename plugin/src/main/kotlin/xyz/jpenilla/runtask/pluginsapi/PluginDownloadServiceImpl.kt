/*
 * Run Task Gradle Plugins
 * Copyright (c) 2023 Jason Penilla
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

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import xyz.jpenilla.runtask.util.Constants
import xyz.jpenilla.runtask.util.Downloader
import xyz.jpenilla.runtask.util.path
import xyz.jpenilla.runtask.util.prettyPrint
import xyz.jpenilla.runtask.util.sha256
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
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
  override fun resolvePlugin(project: Project, download: PluginApiDownload): Path {
    manifest = loadOrCreateManifest()

    return when (download) {
      is HangarApiDownload -> resolveHangerPlugin(project, download)
      is ModrinthApiDownload -> resolveModrinthPlugin(project, download)
      is GitHubApiDownload -> resolveGitHubPlugin(project, download)
    }
  }

  private val refreshDependencies: Boolean
    get() = parameters.refreshDependencies.get()
  private val offlineMode: Boolean
    get() = parameters.offlineMode.get()

  private fun resolveHangerPlugin(project: Project, download: HangarApiDownload): Path {
    val platformType = parameters.platformType.get()
    val cacheDir = parameters.cacheDirectory.get().asFile.toPath()

    val apiUrl = download.url.get().trimEnd('/')
    val apiPlugin = download.plugin.get()
    val apiVersion = download.version.get()

    val provider = manifest.hangarProviders.computeIfAbsent(apiUrl) { HangarProvider() }
    val plugin = provider.computeIfAbsent(apiPlugin) { PluginVersions() }
    val version = plugin[apiVersion] ?: PluginVersion(fileName = "$apiPlugin-$apiVersion.jar")

    val targetDir =
      cacheDir.resolve(Constants.HANGAR_PLUGIN_DIR).resolve(apiPlugin).resolve(apiVersion)
    val targetFile = targetDir.resolve(version.fileName)
    val downloadUrl = "$apiUrl/api/v1/projects/$apiPlugin/versions/$apiVersion/$platformType/download"

    val setter: (PluginVersion) -> Unit = { plugin[apiVersion] = it }

    val ctx = DownloadCtx(project, apiUrl, downloadUrl, targetDir, targetFile, version, setter)
    return download(ctx)
  }

  private fun resolveModrinthPlugin(project: Project, download: ModrinthApiDownload): Path {
    TODO()
  }

  private fun resolveGitHubPlugin(project: Project, download: GitHubApiDownload): Path {
    val cacheDir = parameters.cacheDirectory.get().asFile.toPath()

    val owner = download.owner.get()
    val repo = download.repo.get()
    val tag = download.tag.get()
    val asset = download.assetName.get()

    val ownerProvider = manifest.githubProvider.computeIfAbsent(owner) { GitHubOwner() }
    val repoProvider = ownerProvider.computeIfAbsent(repo) { GitHubRepo() }
    val tagProvider = repoProvider.computeIfAbsent(tag) { PluginVersions() }
    val version = tagProvider[asset] ?: PluginVersion(fileName = asset)

    val targetDir =
      cacheDir.resolve(Constants.GITHUB_PLUGIN_DIR).resolve(owner).resolve(repo).resolve(tag)
    val targetFile = targetDir.resolve(version.fileName)
    val downloadUrl = "https://github.com/$owner/$repo/releases/download/$tag/$asset"

    val setter: (PluginVersion) -> Unit = { tagProvider[asset] = it }

    val ctx = DownloadCtx(project, "github.com", downloadUrl, targetDir, targetFile, version, setter)
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

      if (ctx.targetFile.isRegularFile()) {
        if (ctx.version.lastUpdateCheck > 0 && ctx.version.sha256sum != null && ctx.targetFile.sha256() == ctx.version.sha256sum) {
          // File matches what we expected
          connection.ifModifiedSince = ctx.version.lastUpdateCheck

          if (ctx.version.etag != null) {
            connection.setRequestProperty("If-None-Match", ctx.version.etag)
          }
        } else {
          // The file exists, but we have no way of verifying it
          ctx.targetFile.deleteExisting()
        }
      }

      connection.connect()

      val status = connection.responseCode
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

        val start = Instant.now()
        val opName = "${ctx.baseUrl}:${ctx.version.fileName}"
        when (val res = Downloader(url, ctx.targetFile, ctx.version.fileName, opName).download(ctx.project, connection)) {
          is Downloader.Result.Success -> LOGGER.lifecycle("Done downloading {}, took {}.", ctx.version.fileName, Duration.between(start, Instant.now()).prettyPrint())
          is Downloader.Result.Failure -> throw IllegalStateException("Failed to download ${ctx.version.fileName}.", res.throwable)
        }

        val etagValue: String? = connection.getHeaderField("ETag")
        ctx.setter(ctx.version.copy(lastUpdateCheck = Instant.now().toEpochMilli(), etag = etagValue))
        writeManifest()
        return ctx.targetFile
      }

      throw IllegalStateException("Failed to download ${ctx.version.fileName}, status code: $status")
    } finally {
      connection.disconnect()
    }
  }

  private data class DownloadCtx(
    val project: Project,
    val baseUrl: String,
    val downloadUrl: String,
    val targetDir: Path,
    val targetFile: Path,
    val version: PluginVersion,
    val setter: (PluginVersion) -> Unit
  )
}

// type aliases used to just prevent this from being a horribly nested type to look at
private data class PluginsManifest(
  val hangarProviders: MutableMap<String, HangarProvider> = HashMap(),
  val modrinthProviders: MutableMap<String, ModrinthProvider> = HashMap(),
  val githubProvider: GitHubProvider = GitHubProvider()
)

// hangar aliases:
private typealias HangarProvider = MutableMap<String, PluginVersions>

private fun HangarProvider(): HangarProvider = HashMap()

// modrinth aliases:
private typealias ModrinthProvider = MutableMap<String, PluginVersions>

private fun ModrinthProvider(): ModrinthProvider = HashMap()

// github aliases:
private typealias GitHubProvider = MutableMap<String, GitHubOwner>

private fun GitHubProvider(): GitHubProvider = HashMap()
private typealias GitHubOwner = MutableMap<String, GitHubRepo>

private fun GitHubOwner(): GitHubOwner = HashMap()
private typealias GitHubRepo = MutableMap<String, PluginVersions>

private fun GitHubRepo(): GitHubRepo = HashMap()

// general aliases
private typealias PluginVersions = MutableMap<String, PluginVersion>

private fun PluginVersions(): PluginVersions = HashMap()
private data class PluginVersion(
  val lastUpdateCheck: Long = 0L,
  val etag: String? = null,
  val sha256sum: String? = null,
  val fileName: String
)
