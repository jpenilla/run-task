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
package xyz.jpenilla.runpaper.task

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import xyz.jpenilla.runpaper.minecraftVersionIsSameOrNewerThan
import xyz.jpenilla.runtask.pluginsapi.PluginDownloadService
import xyz.jpenilla.runtask.service.DownloadsAPIService
import xyz.jpenilla.runtask.task.RunWithPlugins
import xyz.jpenilla.runtask.util.FileCopyingPluginHandler
import xyz.jpenilla.runtask.util.spec
import java.io.File
import java.nio.file.Path

/**
 * Task to download and run a Paper server along with plugins.
 *
 * Note that configuring [version] is required for this task type.
 */
public abstract class RunServer : RunWithPlugins() {
  /**
   * Run Paper makes use of Paper's `add-plugin` command line option in order to
   * load the files in [pluginJars] as plugins. This option was implemented during
   * the Minecraft 1.16.5 development cycle, and does not exist in prior versions.
   *
   * Enabling legacy plugin loading instructs Run Paper to copy jars into the plugins
   * folder instead of using the aforementioned command line option, for better
   * compatibility with legacy Minecraft versions.
   *
   * If left un-configured, Run Paper will attempt to automatically
   * determine the appropriate setting based on the configured
   * Minecraft version for this task.
   */
  @get:Optional
  @get:Input
  public abstract val legacyPluginLoading: Property<Boolean>

  override fun init() {
    super.init()

    // Set disable watchdog property for debugging
    systemProperty("disable.watchdog", true)

    downloadsApiService.convention(DownloadsAPIService.paper(project))
    pluginDownloadService.convention(PluginDownloadService.paper(project))
    displayName.convention("Paper")
  }

  override fun resolveBuild(): List<Path> {
    val result = super.resolveBuild()
    if (result.size != 1) {
      // Default main class to CB main when the applied Paperclip classpath is resolved to multiple files
      spec().mainClass.set(mainClass.orElse("org.bukkit.craftbukkit.Main"))
    }
    return result
  }

  override fun preExec(workingDir: Path) {
    super.preExec(workingDir)

    if (!version.isPresent) {
      error("No Minecraft version was specified for the '$name' task!")
    }

    // Disable gui if applicable
    if (version.get().minecraftVersionIsSameOrNewerThan(1, 15)) {
      args("--nogui")
    }

    setupPlugins(workingDir)
  }

  private fun setupPlugins(workingDir: Path) {
    val pluginsDir = workingDir.resolve("plugins")
    val copyingHandler = FileCopyingPluginHandler("RunServer")

    if (addPluginArgumentSupported()) {
      // Delete any jars left over from previous legacy mode runs, even if we are not currently in legacy mode
      copyingHandler.deleteOldPlugins(pluginsDir)

      args(ourPluginJars.files.map { "-add-plugin=${it.absolutePath}" })
    } else {
      copyingHandler.setupPlugins(pluginsDir, ourPluginJars)
    }
  }

  private fun addPluginArgumentSupported(): Boolean {
    if (legacyPluginLoading.isPresent) {
      return !legacyPluginLoading.get()
    }

    return version.get().minecraftVersionIsSameOrNewerThan(1, 16, 5)
  }

  /**
   * Sets the Paper system property to ignore unsupported JVM runtimes.
   * Can allow running older Paper builds on newer JVMs.
   *
   * This may have mixed results based on plugins, Paper version, and more.
   */
  public fun ignoreUnsupportedJvm() {
    systemProperty("Paper.IgnoreJavaVersion", true)
  }

  /**
   * Sets the Minecraft version to use.
   *
   * Convenience method to set the [version] property.
   *
   * @param minecraftVersion Minecraft version
   */
  public fun minecraftVersion(minecraftVersion: String) {
    version(minecraftVersion)
  }

  /**
   * Convenience method for configuring [runClasspath] with a single jar.
   *
   * @param file server jar file
   */
  public fun serverJar(file: File) {
    runJar(file)
  }

  /**
   * Convenience method for configuring [runClasspath] with a single jar.
   *
   * @param file server jar file provider
   */
  public fun serverJar(file: Provider<RegularFile>) {
    runJar(file)
  }

  /**
   * Convenience method setting [legacyPluginLoading] to `true`.
   */
  public fun legacyPluginLoading() {
    legacyPluginLoading.set(true)
  }
}
