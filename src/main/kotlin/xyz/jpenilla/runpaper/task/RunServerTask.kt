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
import de.undercouch.gradle.tasks.download.DownloadAction
import de.undercouch.gradle.tasks.download.DownloadExtension
import de.undercouch.gradle.tasks.download.VerifyAction
import de.undercouch.gradle.tasks.download.VerifyExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.delegateClosureOf
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.hasPlugin
import org.gradle.kotlin.dsl.property
import org.gradle.process.JavaExecSpec
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
  private val pluginJar: RegularFileProperty = this.project.objects.fileProperty().convention { this.resolvePluginJar() }
  private val pluginJarName: Property<String> = this.project.objects.property<String>().convention(this.project.name + ".jar")
  private val paperclipJar: RegularFileProperty = this.project.objects.fileProperty().convention(this.runDirectory.file("paperclip.jar"))

  @TaskAction
  private fun runServer() {
    this.beforeRun()
    this.project.javaexec(this::configureJavaExec)
  }

  private fun configureJavaExec(javaExec: JavaExecSpec) {
    javaExec.workingDir(this.runDirectory)
    javaExec.standardInput = System.`in`
    javaExec.args = listOf("nogui")
    javaExec.systemProperty("disable.watchdog", true)
    javaExec.classpath(this.paperclipJar)
  }

  private fun beforeRun() {
    // Create working dir if needed
    val workingDir = this.runDirectory.get().asFile
    if (!workingDir.exists()) {
      workingDir.mkdirs()
    }

    // Create plugins dir if needed and copy plugin jar
    val pluginsDir = workingDir.resolve("plugins")
    if (!pluginsDir.exists()) {
      pluginsDir.mkdir()
    }
    val plugin = pluginsDir.resolve(this.pluginJarName.get())
    this.pluginJar.get().asFile.copyTo(plugin, overwrite = true)

    this.downloadPaper()
  }

  private fun downloadPaper() {
    // todo: keep a versioned cache
    if (this.paperBuild.get() == PaperBuild.LATEST && this.paperclipJar.get().asFile.exists() && this.paperclipJar.get().asFile.lastModified() > System.currentTimeMillis() - Duration.ofDays(3).toMillis()) {
      return
    }
    val downloadsApi = DownloadsAPI()

    val buildNumber = if (this.paperBuild.get() == PaperBuild.LATEST) {
      downloadsApi.version(Projects.PAPER, this.minecraftVersion.get()).builds.last()
    } else {
      this.paperBuild.get().buildNumber
    }
    val download = downloadsApi.build(Projects.PAPER, this.minecraftVersion.get(), buildNumber).downloads.entries.first().value

    this.logger.lifecycle("Downloading Paper {} build {}...", this.minecraftVersion.get(), buildNumber)
    this.download {
      this.src(downloadsApi.downloadURL(Projects.PAPER, this@RunServerTask.minecraftVersion.get(), buildNumber, download))
      this.dest(this@RunServerTask.paperclipJar.get().asFile)
    }
    this.logger.lifecycle("Done downloading Paper.")
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
   * Configures the filename for the plugin jar in the test server plugins
   * directory. Should include the `.jar` extension. Defaults to `${project.name}.jar`.
   *
   * @param pluginJarName new plugin jar name
   */
  public fun pluginJarName(pluginJarName: String) {
    this.pluginJarName.set(pluginJarName)
  }

  /**
   * Configures the file to use for the plugin jar. By default will
   * attempt to use `shadowJar` if present, else the standard `jar`.
   *
   * @param pluginJar new plugin jar file
   */
  public fun pluginJar(pluginJar: File) {
    this.pluginJar.set(pluginJar)
  }

  /**
   * Configures the file to use for the plugin jar. By default will
   * attempt to use `shadowJar` if present, else the standard `jar`.
   *
   * @param pluginJar new plugin jar file
   */
  public fun pluginJar(pluginJar: RegularFileProperty) {
    this.pluginJar.set(pluginJar)
  }

  public class PaperBuild internal constructor(internal val buildNumber: Int) {
    public companion object {
      public val LATEST: PaperBuild = PaperBuild(-1)
    }
  }

  internal fun resolveTaskDependency(): AbstractArchiveTask? {
    if (this.project.plugins.hasPlugin(ShadowPlugin::class)) {
      return this.project.tasks.getByName<ShadowJar>("shadowJar")
    }
    return this.project.tasks.findByName("jar") as? AbstractArchiveTask
  }

  private fun resolvePluginJar(): File {
    val task = this.resolveTaskDependency()
      ?: error("Could not resolve plugin jar automatically, you will need to manually specify it's location, and possibly add task dependencies.")
    return task.archiveFile.get().asFile
  }

  private fun download(action: DownloadAction.() -> Unit) =
    this.project.extensions.getByType<DownloadExtension>().configure(delegateClosureOf(action))

  private fun verify(action: VerifyAction.() -> Unit) =
    this.project.extensions.getByType<VerifyExtension>().configure(delegateClosureOf(action))
}
