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
package xyz.jpenilla.runtask.service

import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.kotlin.dsl.registerIfAbsent
import xyz.jpenilla.runtask.paperapi.DownloadsAPI
import xyz.jpenilla.runtask.paperapi.Projects
import xyz.jpenilla.runtask.util.Constants
import xyz.jpenilla.runtask.util.InvalidDurationException
import xyz.jpenilla.runtask.util.capitalized
import xyz.jpenilla.runtask.util.parseDuration
import xyz.jpenilla.runtask.util.set
import xyz.jpenilla.runtask.util.sharedCaches
import java.nio.file.Path
import java.time.Duration

/**
 * Service that downloads and caches jars. The included implementation is for the Paper
 * Fill API v3.
 */
public interface DownloadsAPIService {
  /**
   * Resolve a build.
   *
   * This method should be thread safe.
   *
   * @param progressLoggerFactory progress logger factory
   * @param version version string
   * @param build build to resolve
   */
  public fun resolveBuild(
    progressLoggerFactory: ProgressLoggerFactory,
    version: String,
    build: Build
  ): Path

  public companion object {
    private val LOGGER = Logging.getLogger(Companion::class.java)

    /**
     * Registers a [DownloadsAPIService] with the provided configuration. If
     * there is already a [DownloadsAPIService] registered with the configured
     * name, it will be returned instead.
     *
     * @param project project
     * @param op builder configurer
     */
    public fun registerIfAbsent(
      project: Project,
      op: Action<RegistrationBuilder>
    ): Provider<out DownloadsAPIService> {
      val builder = RegistrationBuilderImpl()
      op.execute(builder)
      val proj = requireNotNull(builder.downloadProjectName) { "Missing downloadProjectName" }
      val endpoint = requireNotNull(builder.downloadsEndpoint) { "Missing downloadsEndpoint" }
      val serviceName = builder.buildServiceName ?: "$proj-downloads_api_service"
      val cacheDir = builder.cacheOverride
        ?: project.sharedCaches.resolve(Constants.USER_PATH).resolve(serviceName)
      return project.gradle.sharedServices.registerIfAbsent(serviceName, DownloadsAPIServiceImpl::class) {
        parameters {
          downloadsEndpoint.set(endpoint)
          downloadProject.set(proj)
          downloadProjectDisplayName.set(builder.downloadProjectDisplayName ?: proj.defaultDisplayName())
          cacheDirectory.set(cacheDir)
          refreshDependencies.set(project.gradle.startParameter.isRefreshDependencies)
          updateCheckFrequency.set(updateCheckFrequency(project))
          offlineMode.set(project.gradle.startParameter.isOffline)
        }
      }
    }

    private fun String.defaultDisplayName(): String =
      split('-').joinToString(" ") { it.capitalized() }

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

    /**
     * Get the default [DownloadsAPIService] used to download Paper.
     *
     * @param project project
     */
    public fun paper(project: Project): Provider<out DownloadsAPIService> = registerIfAbsent(project) {
      this as RegistrationBuilderImpl
      downloadsEndpoint = DownloadsAPI.PAPER_ENDPOINT
      downloadProjectName = Projects.PAPER
      buildServiceName = Constants.Services.PAPER
      cacheOverride = project.sharedCaches.resolve(Constants.PAPER_PATH)
    }

    /**
     * Get the default [DownloadsAPIService] used to download Folia.
     *
     * @param project project
     */
    public fun folia(project: Project): Provider<out DownloadsAPIService> = registerIfAbsent(project) {
      this as RegistrationBuilderImpl
      downloadsEndpoint = DownloadsAPI.PAPER_ENDPOINT
      downloadProjectName = Projects.FOLIA
      buildServiceName = Constants.Services.FOLIA
      cacheOverride = project.sharedCaches.resolve(Constants.FOLIA_PATH)
    }

    /**
     * Get the default [DownloadsAPIService] used to download Velocity.
     *
     * @param project project
     */
    public fun velocity(project: Project): Provider<out DownloadsAPIService> = registerIfAbsent(project) {
      this as RegistrationBuilderImpl
      downloadsEndpoint = DownloadsAPI.PAPER_ENDPOINT
      downloadProjectName = Projects.VELOCITY
      buildServiceName = Constants.Services.VELOCITY
      cacheOverride = project.sharedCaches.resolve(Constants.VELOCITY_PATH)
    }

    /**
     * Get the default [DownloadsAPIService] used to download Waterfall.
     *
     * @param project project
     */
    public fun waterfall(project: Project): Provider<out DownloadsAPIService> = registerIfAbsent(project) {
      this as RegistrationBuilderImpl
      downloadsEndpoint = DownloadsAPI.PAPER_ENDPOINT
      downloadProjectName = Projects.WATERFALL
      buildServiceName = Constants.Services.WATERFALL
      cacheOverride = project.sharedCaches.resolve(Constants.WATERFALL_PATH)
    }

    private class RegistrationBuilderImpl(
      override var buildServiceName: String? = null,
      override var downloadsEndpoint: String? = null,
      override var downloadProjectName: String? = null,
      override var downloadProjectDisplayName: String? = null,
      var cacheOverride: Path? = null
    ) : RegistrationBuilder
  }

  /**
   * Builder for [DownloadsAPIService] registration.
   */
  public interface RegistrationBuilder {
    /**
     * Name for the build service, will be derived from [downloadProjectName] if null.
     */
    public var buildServiceName: String?

    /**
     * Paper Fill API v3 endpoint.
     */
    public var downloadsEndpoint: String?

    /**
     * Name of the project to download.
     */
    public var downloadProjectName: String?

    /**
     * Display name for the downloaded project, will be derived from [downloadProjectName] if null.
     */
    public var downloadProjectDisplayName: String?
  }

  /**
   * Represents a build of a Paper Fill API v3 project.
   */
  public sealed class Build {
    /**
     * [Build] pointing to the latest build for a version.
     */
    public object Latest : Build()

    public data class Specific(public val buildNumber: Int) : Build()
  }
}
