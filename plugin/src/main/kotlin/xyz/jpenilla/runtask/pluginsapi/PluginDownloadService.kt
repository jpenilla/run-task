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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.kotlin.dsl.registerIfAbsent
import xyz.jpenilla.runtask.paperapi.Projects
import xyz.jpenilla.runtask.util.Constants
import xyz.jpenilla.runtask.util.sharedCaches
import java.nio.file.Path

/**
 * Service that downloads and caches plugin jars. The included implementation is for the Hangar API, Modrinth API,
 * GitHub Releases, and plain URLs.
 */
public interface PluginDownloadService : BuildService<PluginDownloadService.Parameters> {
  public interface Parameters : BuildServiceParameters {
    public val platformType: Property<PlatformType>
    public val cacheDirectory: DirectoryProperty
    public val refreshDependencies: Property<Boolean>
    public val offlineMode: Property<Boolean>
  }

  public enum class PlatformType {
    PAPER,
    VELOCITY,
    WATERFALL
  }

  /**
   * Resolve a plugin.
   *
   * @param project project to use for context
   * @param download plugin download
   */
  public fun resolvePlugin(progressLoggerFactory: ProgressLoggerFactory, download: PluginApiDownload): Path

  public companion object {
    public fun registerIfAbsent(namePrefix: String, project: Project, config: Action<in Parameters>): Provider<out PluginDownloadService> {
      val serviceName = "$namePrefix-plugin_download_service"
      return project.gradle.sharedServices.registerIfAbsent(serviceName, PluginDownloadServiceImpl::class) {
        config.execute(parameters)
        parameters {
          refreshDependencies.set(project.gradle.startParameter.isRefreshDependencies)
          offlineMode.set(project.gradle.startParameter.isOffline)
        }
      }
    }

    /**
     * Get the default [PluginDownloadService] used to download Paper/Folia plugins.
     *
     * @param project project
     */
    public fun paper(project: Project): Provider<out PluginDownloadService> {
      return registerIfAbsent(Projects.PAPER, project) {
        platformType.set(PlatformType.PAPER)
        cacheDirectory.fileValue(project.sharedCaches.resolve(Constants.PAPER_PLUGINS_PATH).toFile())
      }
    }

    /**
     * Get the default [PluginDownloadService] used to download Velocity plugins.
     *
     * @param project project
     */
    public fun velocity(project: Project): Provider<out PluginDownloadService> {
      return registerIfAbsent(Projects.VELOCITY, project) {
        platformType.set(PlatformType.VELOCITY)
        cacheDirectory.fileValue(project.sharedCaches.resolve(Constants.VELOCITY_PLUGINS_PATH).toFile())
      }
    }

    /**
     * Get the default [PluginDownloadService] used to download Waterfall plugins.
     *
     * @param project project
     */
    public fun waterfall(project: Project): Provider<out PluginDownloadService> {
      return registerIfAbsent(Projects.WATERFALL, project) {
        platformType.set(PlatformType.WATERFALL)
        cacheDirectory.fileValue(project.sharedCaches.resolve(Constants.WATERFALL_PLUGINS_PATH).toFile())
      }
    }
  }
}
