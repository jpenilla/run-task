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
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.hasPlugin
import org.gradle.kotlin.dsl.property
import org.gradle.process.JavaExecSpec
import xyz.jpenilla.runpaper.extension.download
import xyz.jpenilla.runpaper.extension.verify
import xyz.jpenilla.runpaper.paperapi.DownloadsAPI
import xyz.jpenilla.runpaper.paperapi.Projects
import java.io.File
import java.time.Duration

/**
 * Task to download and run a Paper server along with a plugin.
 */
@Suppress("unused")
public open class RunServerTask : DefaultTask() {
  private val minecraftVersion: Property<String> = this.project.objects.property()
  private val paperBuild: Property<PaperBuild> = this.project.objects.property<PaperBuild>().convention(PaperBuild.LATEST)
  private val runDirectory: DirectoryProperty = this.project.objects.directoryProperty().convention(this.project.layout.projectDirectory.dir("run"))
  private val paperclipJar: RegularFileProperty = this.project.objects.fileProperty().convention(this.runDirectory.file("paperclip.jar"))

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

  @TaskAction
  private fun runServer() {
    this.beforeRun()
    this.project.javaexec(this::configureJavaExec)
  }

  private fun configureJavaExec(javaExec: JavaExecSpec) {
    javaExec.standardInput = System.`in`
    javaExec.workingDir(this.runDirectory)
    javaExec.classpath(this.paperclipJar)

    // Set disable watchdog property for debugging
    javaExec.systemProperty("disable.watchdog", true)

    // Add our arguments
    val arguments = mutableListOf("nogui")
    arguments.addAll(this.pluginJars.files.map { "-add-plugin=${it.absolutePath}" })
    javaExec.args = arguments
  }

  private fun beforeRun() {
    // Create working dir if needed
    val workingDir = this.runDirectory.get().asFile
    if (!workingDir.exists()) {
      workingDir.mkdirs()
    }

    this.downloadPaper()
  }

  private fun downloadPaper() {
    // todo: keep a versioned cache
    if (this.paperBuild.get() == PaperBuild.LATEST && this.paperclipJar.get().asFile.exists() && this.paperclipJar.get().asFile.lastModified() > System.currentTimeMillis() - Duration.ofDays(3).toMillis()) {
      return
    }
    val downloadsApi = DownloadsAPI()

    // Find our build
    val buildNumber = if (this.paperBuild.get() == PaperBuild.LATEST) {
      downloadsApi.version(Projects.PAPER, this.minecraftVersion.get()).builds.last()
    } else {
      this.paperBuild.get().buildNumber
    }
    val download = downloadsApi.build(Projects.PAPER, this.minecraftVersion.get(), buildNumber).downloads.entries.first().value

    // Download
    this.logger.lifecycle("Downloading Paper {} build {}...", this.minecraftVersion.get(), buildNumber)
    this.download {
      this.src(downloadsApi.downloadURL(Projects.PAPER, this@RunServerTask.minecraftVersion.get(), buildNumber, download))
      this.dest(this@RunServerTask.paperclipJar.get().asFile)
    }
    this.logger.lifecycle("Done downloading Paper.")

    // Verify
    this.verify {
      this.algorithm("SHA256")
      this.checksum(download.sha256)
      this.src(this@RunServerTask.paperclipJar.get().asFile)
    }
    this.logger.lifecycle("Verified SHA256 hash of downloaded jar.")
  }

  /**
   * Sets the Minecraft version to use.
   *
   * @param minecraftVersion
   */
  public fun minecraftVersion(minecraftVersion: String) {
    this.minecraftVersion.set(minecraftVersion)
  }

  /**
   * Sets the build of Paper to use. By default, [PaperBuild.LATEST] is
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
    this.paperBuild.set(PaperBuild(paperBuildNumber))
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
   * Gets the currently configured run directory
   * for the test server.
   *
   * @return currently configured run directory
   */
  public fun runDirectory(): File {
    return this.runDirectory.get().asFile
  }

  /**
   * Represents a build of Paper.
   */
  public class PaperBuild internal constructor(internal val buildNumber: Int) {
    public companion object {
      /**
       * [PaperBuild] instance pointing to the latest Paper build for the configured Minecraft version.
       */
      public val LATEST: PaperBuild = PaperBuild(-1)
    }
  }

  internal fun resolveTaskDependency(): AbstractArchiveTask? {
    if (this.project.plugins.hasPlugin(ShadowPlugin::class)) {
      return this.project.tasks.getByName<ShadowJar>("shadowJar")
    }
    return this.project.tasks.findByName("jar") as? AbstractArchiveTask
  }

  internal fun resolvePluginJar(): File? =
    this.resolveTaskDependency()?.archiveFile?.get()?.asFile
}
