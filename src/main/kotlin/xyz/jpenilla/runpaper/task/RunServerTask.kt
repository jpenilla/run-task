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
package xyz.jpenilla.runpaper.task

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildServiceRegistration
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.hasPlugin
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.property
import xyz.jpenilla.runpaper.Constants
import xyz.jpenilla.runpaper.service.PaperclipService
import java.io.File

/**
 * Task to download and run a Paper server along with plugins.
 */
@Suppress("unused")
public abstract class RunServerTask : JavaExec() {
  private val minecraftVersion: Property<String> = this.project.objects.property()
  private val paperBuild: Property<PaperBuild> = this.project.objects.property<PaperBuild>().convention(PaperBuild.Latest)
  private val paperclipService: Provider<PaperclipService> = this.project.gradle.sharedServices.registrations
    .named<BuildServiceRegistration<PaperclipService, PaperclipService.Parameters>>(Constants.Services.PAPERCLIP).flatMap { it.service }
  private val paperclipJar: RegularFileProperty = this.project.objects.fileProperty()

  /**
   * The run directory for the test server.
   * Defaults to `run` in the project directory.
   */
  @Internal
  public val runDirectory: DirectoryProperty = this.project.objects.directoryProperty().convention(this.project.layout.projectDirectory.dir("run"))

  /**
   * The collection of plugin jars to load. Run Paper will attempt to locate
   * a plugin jar from the shadowJar task output if present, or else the standard
   * jar archive. In non-standard setups, it may be necessary to manually add
   * your plugin's jar to this collection, as well as specify task dependencies.
   *
   * Adding files to this collection may also be useful for projects which produce
   * more than one plugin jar, or to load dependency plugins.
   */
  @InputFiles
  public val pluginJars: ConfigurableFileCollection = this.project.objects.fileCollection()

  override fun exec() {
    this.configure()
    this.beforeExec()
    this.logger.lifecycle("Starting Paper...")
    this.logger.lifecycle("")
    super.exec()
  }

  private fun configure() {
    this.standardInput = System.`in`
    this.workingDir(this.runDirectory)
    val paperclip = this.paperclipJar.orElse {
      this.paperclipService.get().resolvePaperclip(
        this.project,
        this.minecraftVersion.get(),
        this.paperBuild.get()
      )
    }.get().asFile
    this.classpath(paperclip)

    // Set disable watchdog property for debugging
    this.systemProperty("disable.watchdog", true)

    this.systemProperty("net.kyori.adventure.text.warnWhenLegacyFormattingDetected", true)

    // Add our arguments
    this.args("--nogui")
    this.args(this.pluginJars.files.map { "-add-plugin=${it.absolutePath}" })
  }

  private fun beforeExec() {
    // Create working dir if needed
    val workingDir = this.runDirectory.get().asFile
    if (!workingDir.exists()) {
      workingDir.mkdirs()
    }
  }

  /**
   * Sets the Minecraft version to use.
   *
   * @param minecraftVersion minecraft version
   */
  public fun minecraftVersion(minecraftVersion: String) {
    this.minecraftVersion.set(minecraftVersion)
  }

  /**
   * Sets the build of Paper to use. By default, [PaperBuild.Latest] is
   * used, which uses the latest build for the configured Minecraft version.
   *
   * @param paperBuild paper build
   */
  public fun paperBuild(paperBuild: PaperBuild) {
    this.paperBuild.set(paperBuild)
  }

  /**
   * Sets a specific build number of Paper to use. By default the latest
   * build for the configured Minecraft version is used.
   *
   * @param paperBuildNumber build number
   */
  public fun paperBuild(paperBuildNumber: Int) {
    this.paperBuild.set(PaperBuild.Specific(paperBuildNumber))
  }

  /**
   * Sets the run directory for the test server.
   * Defaults to `run` in the project directory.
   *
   * @param runDirectory run directory
   */
  public fun runDirectory(runDirectory: File) {
    this.runDirectory.set(runDirectory)
  }

  /**
   * Sets a custom Paperclip to use for this task. By default,
   * Run Paper will resolve a Paperclip using the Paper downloads
   * API, however you can use this function to override that
   * behavior.
   *
   * @param file paperclip file
   */
  public fun paperclip(file: File) {
    this.paperclipJar.set(file)
  }

  /**
   * Sets a custom Paperclip to use for this task. By default,
   * Run Paper will resolve a Paperclip using the Paper downloads
   * API, however you can use this function to override that
   * behavior.
   *
   * @param file paperclip file provider
   */
  public fun paperclip(file: Provider<RegularFile>) {
    this.paperclipJar.set(file)
  }

  /**
   * Convenience method for easily adding jars to [pluginJars].
   *
   * @param jars jars to add
   */
  public fun pluginJars(vararg jars: File) {
    this.pluginJars.from(jars)
  }

  /**
   * Convenience method for easily adding jars to [pluginJars].
   *
   * @param jars jars to add
   */
  public fun pluginJars(vararg jars: Provider<RegularFile>) {
    this.pluginJars.from(jars)
  }

  /**
   * Represents a build of Paper.
   */
  public sealed class PaperBuild {
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

  internal fun resolveTaskDependency(): AbstractArchiveTask? {
    if (this.project.plugins.hasPlugin(ShadowPlugin::class)) {
      return this.project.tasks.getByName<ShadowJar>("shadowJar")
    }
    return this.project.tasks.findByName("jar") as? AbstractArchiveTask
  }
}
