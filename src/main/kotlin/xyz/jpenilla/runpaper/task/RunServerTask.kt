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
package xyz.jpenilla.runpaper.task

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.property
import xyz.jpenilla.runpaper.service.PaperclipService
import xyz.jpenilla.runpaper.util.path
import java.io.File
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * Task to download and run a Paper server along with plugins.
 */
@Suppress("unused")
public abstract class RunServerTask : JavaExec() {
  /**
   * The build of Paper to use.
   */
  @get: Internal
  public val paperBuild: Property<PaperclipService.Build> = objectFactory.property<PaperclipService.Build>().convention(PaperclipService.Build.Latest)

  /**
   * The Minecraft version for this [RunServerTask]. This version will be used
   * for resolving Paperclips from the Paper downloads API, as well as for
   * version-specific logic when launching the server.
   */
  @get:Input
  public abstract val minecraftVersion: Property<String>

  /**
   * Setting this property allows configuring a custom jar file to start the
   * server from. If left un-configured, Run Paper will resolve a Paperclip
   * using the Paper downloads API.
   */
  @get:Optional
  @get:InputFile
  public abstract val serverJar: RegularFileProperty

  /**
   * This property allows configuring a custom classpath to start the
   * server from. If left empty, Run Paper will resolve a Paperclip
   * using the Paper downloads API.
   *
   * This property and [serverJar] are mutually exclusive, meaning you may
   * configure one or the other, but not both.
   *
   * When configuring this property to have more than one entry, ensure that
   * [mainClass] has also been set.
   */
  @get:Classpath
  public abstract val serverClasspath: ConfigurableFileCollection

  /**
   * Service used to resolve Paper when [serverJar] and [serverClasspath] are not configured.
   */
  @get:Internal
  public val paperclipService: Property<PaperclipService> = objectFactory.property<PaperclipService>().convention(PaperclipService.paper(project))

  /**
   * Used in startup log.
   */
  @get:Internal
  public val serverDisplayName: Property<String> = objectFactory.property<String>().convention("Paper")

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

  /**
   * The run directory for the test server.
   * Defaults to `run` in the project directory.
   */
  @Internal
  public val runDirectory: DirectoryProperty = objectFactory.directoryProperty().convention(project.layout.projectDirectory.dir("run"))

  /**
   * The collection of plugin jars to load. Run Paper will attempt to locate
   * a plugin jar from the shadowJar task output if present, or else the standard
   * jar archive. In non-standard setups, it may be necessary to manually add
   * your plugin's jar to this collection, as well as specify task dependencies.
   *
   * Adding files to this collection may also be useful for projects which produce
   * more than one plugin jar, or to load dependency plugins.
   */
  @get:InputFiles
  public abstract val pluginJars: ConfigurableFileCollection

  override fun exec() {
    configure()
    beforeExec()
    logger.lifecycle("Starting {}...", serverDisplayName.get())
    logger.lifecycle("")
    super.exec()
  }

  private fun configure() {
    if (!minecraftVersion.isPresent) {
      error("No Minecraft version was specified for the '$name' task!")
    }

    standardInput = System.`in`
    workingDir(runDirectory)

    if (serverJar.isPresent && !serverClasspath.isEmpty) {
      error("serverJar and serverClasspath must not be used at the same time!")
    }

    val selectedServerClasspath = if (serverJar.isPresent) {
      serverJar.path
    } else if (!serverClasspath.isEmpty) {
      serverClasspath
    } else {
      paperclipService.get().resolvePaperclip(
        project,
        minecraftVersion.get(),
        paperBuild.get()
      )
    }
    classpath(selectedServerClasspath)

    // Set disable watchdog property for debugging
    systemProperty("disable.watchdog", true)

    systemProperty("net.kyori.adventure.text.warnWhenLegacyFormattingDetected", true)

    // Add our arguments
    if (minecraftVersionIsSameOrNewerThan(1, 15)) {
      args("--nogui")
    }
  }

  private fun beforeExec() {
    // Create working dir if needed
    val workingDir = runDirectory.path
    if (!workingDir.isDirectory()) {
      workingDir.createDirectories()
    }

    val plugins = workingDir.resolve("plugins")
    if (!plugins.isDirectory()) {
      plugins.createDirectories()
    }

    val prefix = "_run-paper_plugin_"
    val extension = ".jar"

    // Delete any jars left over from previous legacy mode runs
    plugins.listDirectoryEntries()
      .filter { it.isRegularFile() && it.name.startsWith(prefix) && it.name.endsWith(extension) }
      .forEach { it.deleteIfExists() }

    // Add plugins
    if (addPluginArgumentSupported()) {
      args(pluginJars.files.map { "-add-plugin=${it.absolutePath}" })
    } else {
      pluginJars.files.map { it.toPath() }.forEachIndexed { i, jar ->
        jar.copyTo(plugins.resolve(prefix + i + extension))
      }
    }
  }

  private fun addPluginArgumentSupported(): Boolean {
    if (legacyPluginLoading.isPresent) {
      return !legacyPluginLoading.get()
    }

    return minecraftVersionIsSameOrNewerThan(1, 16, 5)
  }

  private fun minecraftVersionIsSameOrNewerThan(vararg other: Int): Boolean {
    val minecraft = minecraftVersion.get().split(".").map {
      try {
        it.toInt()
      } catch (ex: NumberFormatException) {
        return true
      }
    }

    for ((current, target) in minecraft zip other.toList()) {
      if (current < target) return false
      if (current > target) return true
      // If equal, check next subversion
    }

    // version is same
    return true
  }

  /**
   * Sets the Minecraft version to use.
   *
   * @param version minecraft version
   */
  public fun minecraftVersion(version: String) {
    minecraftVersion.set(version)
  }

  /**
   * Sets the build of Paper to use. By default, [PaperBuild.Latest] is
   * used, which uses the latest build for the configured Minecraft version.
   *
   * @param build paper build
   */
  @Deprecated("PaperBuild has been deprecated, use PaperclipService.Build instead")
  public fun paperBuild(build: PaperBuild) {
    paperBuild(build.asNew())
  }

  /**
   * Sets the build of Paper to use. By default, [PaperclipService.Build.Latest] is
   * used, which uses the latest build for the configured Minecraft version.
   *
   * @param build paper build
   */
  public fun paperBuild(build: PaperclipService.Build) {
    paperBuild.set(build)
  }

  /**
   * Sets a specific build number of Paper to use. [PaperclipService.Build.Latest] is
   * used, which uses the latest build for the configured Minecraft version.
   *
   * @param paperBuildNumber build number
   */
  public fun paperBuild(paperBuildNumber: Int) {
    paperBuild.set(PaperclipService.Build.Specific(paperBuildNumber))
  }

  /**
   * Sets the run directory for the test server.
   * Defaults to `run` in the project directory.
   *
   * @param directory run directory
   */
  public fun runDirectory(directory: File) {
    runDirectory.set(directory)
  }

  /**
   * Convenience method for configuring the [serverJar] property.
   *
   * @param file server jar file
   */
  @Deprecated("Replaced by serverJar.", replaceWith = ReplaceWith("serverJar(file)"))
  public fun paperclip(file: File) {
    serverJar.set(file)
  }

  /**
   * Convenience method for configuring the [serverJar] property.
   *
   * @param file server jar file provider
   */
  @Deprecated("Replaced by serverJar.", replaceWith = ReplaceWith("serverJar(file)"))
  public fun paperclip(file: Provider<RegularFile>) {
    serverJar.set(file)
  }

  /**
   * Convenience method for configuring the [serverJar] property.
   *
   * @param file server jar file
   */
  public fun serverJar(file: File) {
    serverJar.set(file)
  }

  /**
   * Convenience method for configuring the [serverJar] property.
   *
   * @param file server jar file provider
   */
  public fun serverJar(file: Provider<RegularFile>) {
    serverJar.set(file)
  }

  /**
   * Convenience method for easily adding jars to [pluginJars].
   *
   * @param jars jars to add
   */
  public fun pluginJars(vararg jars: File) {
    pluginJars.from(jars)
  }

  /**
   * Convenience method for easily adding jars to [pluginJars].
   *
   * @param jars jars to add
   */
  public fun pluginJars(vararg jars: Provider<RegularFile>) {
    pluginJars.from(jars)
  }

  /**
   * Convenience method setting [legacyPluginLoading] to `true`.
   */
  public fun legacyPluginLoading() {
    legacyPluginLoading.set(true)
  }

  /**
   * Represents a build of Paper.
   */
  @Deprecated(
    "Replaced by PaperclipService.Build",
    replaceWith = ReplaceWith("PaperclipService.Build", "xyz.jpenilla.runpaper.service.PaperclipService")
  )
  public sealed class PaperBuild {
    internal fun asNew(): PaperclipService.Build = when (this) {
      is Latest -> PaperclipService.Build.Latest
      is Specific -> PaperclipService.Build.Specific(buildNumber)
    }

    public companion object {
      /**
       * [PaperBuild] instance pointing to the latest Paper build for the configured Minecraft version.
       */
      @Deprecated("Replaced by RunServerTask.PaperBuild.Latest.", replaceWith = ReplaceWith("RunServerTask.PaperBuild.Latest"))
      public val LATEST: PaperBuild = Latest
    }

    /**
     * [PaperBuild] pointing to the latest Paper build for the configured Minecraft version.
     */
    public object Latest : PaperBuild()

    public data class Specific internal constructor(internal val buildNumber: Int) : PaperBuild()
  }
}
