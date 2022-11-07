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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildServiceRegistration
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.registerIfAbsent
import xyz.jpenilla.runpaper.Constants
import xyz.jpenilla.runpaper.util.set
import xyz.jpenilla.runpaper.util.sharedCaches
import java.nio.file.Path

/**
 * Service that downloads and caches Paperclip jars.
 */
public interface PaperclipService {
  /**
   * Resolve a Paperclip.
   *
   * @param project project to use for context
   * @param minecraftVersion minecraft version string
   * @param paperBuild build to resolve
   */
  public fun resolvePaperclip(
    project: Project,
    minecraftVersion: String,
    paperBuild: Build
  ): Path

  public companion object {
    /**
     * Registers a [PaperclipService] with the provided configuration. If
     * there is already a [PaperclipService] registered with the configured
     * name, it will be returned instead.
     *
     * @param project project
     * @param op builder configurer
     */
    public fun register(
      project: Project,
      op: Action<RegistrationBuilder>
    ): Provider<out PaperclipService> {
      val builder = object : RegistrationBuilder {
        override var buildServiceName: String? = null
        override var downloadsEndpoint: String? = null
        override var downloadProjectName: String? = null
        override var downloadProjectDisplayName: String? = null
      }
      op.execute(builder)
      val proj = requireNotNull(builder.downloadProjectName) { "Missing downloadProjectName" }
      val endpoint = requireNotNull(builder.downloadsEndpoint) { "Missing downloadsEndpoint" }
      val serviceName = builder.buildServiceName ?: "$proj-paperclip_service"
      return project.gradle.sharedServices.registerIfAbsent(serviceName, PaperclipServiceImpl::class) {
        maxParallelUsages.set(1)
        parameters.downloadsEndpoint.set(endpoint)
        parameters.downloadProject.set(proj)
        parameters.downloadProjectDisplayName.set(builder.downloadProjectDisplayName ?: proj.defaultDisplayName())
        parameters.cacheDirectory.set(
          project.sharedCaches.resolve(Constants.RUN_PAPER_PATH).run {
            if (serviceName == Constants.Services.PAPERCLIP) this else resolve(serviceName)
          }
        )
        parameters.refreshDependencies.set(project.gradle.startParameter.isRefreshDependencies)
        parameters.offlineMode.set(project.gradle.startParameter.isOffline)
      }
    }

    private fun String.defaultDisplayName(): String =
      split(' ').joinToString(" ") { it.capitalize() }

    /**
     * Get the default [PaperclipService] used to download Paper.
     *
     * @param project project
     */
    public fun paper(project: Project): Provider<out PaperclipService> =
      project.gradle.sharedServices.registrations
        .named<BuildServiceRegistration<PaperclipServiceImpl, PaperclipServiceImpl.Parameters>>(Constants.Services.PAPERCLIP)
        .flatMap { it.service }
  }

  /**
   * Builder for [PaperclipService] registration.
   */
  public interface RegistrationBuilder {
    /**
     * Name for the build service, will be derived from [downloadProjectName] if null.
     */
    public var buildServiceName: String?

    /**
     * Paper downloads API v2 endpoint.
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
   * Represents a build of Paper.
   */
  public sealed class Build {
    /**
     * [Build] pointing to the latest Paper build for the configured Minecraft version.
     */
    public object Latest : Build()

    public data class Specific internal constructor(internal val buildNumber: Int) : Build()
  }
}
