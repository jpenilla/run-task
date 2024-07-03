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
package xyz.jpenilla.runtask.task

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import xyz.jpenilla.runtask.service.DownloadsAPIService
import xyz.jpenilla.runtask.util.path
import java.io.File
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory

/**
 * Base run task which can fetch a runtime jar using [DownloadsAPIService].
 */
public abstract class AbstractRun : JavaExec() {
  /**
   * The build to resolve. By default, [DownloadsAPIService.Build.Latest] is
   * used, which uses the latest build for the configured [version].
   */
  @get:Internal
  public abstract val build: Property<DownloadsAPIService.Build>

  /**
   * The version of software being run by this [AbstractRun]. It is only used
   * to know what version to download using [downloadsApiService], however
   * subclasses may use it for further purposes.
   */
  @get:Input
  @get:Optional
  public abstract val version: Property<String>

  /**
   * This property allows configuring a custom classpath to start the
   * run task from. If left empty, a jar will be resolved using [downloadsApiService].
   *
   * When configuring this property to have more than one entry, ensure that
   * [mainClass] has also been set.
   */
  @get:Classpath
  public abstract val runClasspath: ConfigurableFileCollection

  /**
   * Service used to resolve a jar when [runClasspath] is not configured.
   */
  @get:Internal
  public abstract val downloadsApiService: Property<DownloadsAPIService>

  /**
   * Used in startup log.
   */
  @get:Internal
  public abstract val displayName: Property<String>

  /**
   * The run directory for the test server.
   *
   * Defaults to `run` in the project directory.
   */
  @get:Internal
  public abstract val runDirectory: DirectoryProperty

  @get:Inject
  protected abstract val layout: ProjectLayout

  init {
    init0()
  }

  private fun init0() {
    runDirectory.convention(layout.projectDirectory.dir("run"))
    build.convention(DownloadsAPIService.Build.Latest)
    systemProperty("net.kyori.adventure.text.warnWhenLegacyFormattingDetected", true)
    systemProperty("xyz.jpenilla.run-task", true)
    systemProperty("file.encoding", "UTF-8") // #60
    init()
  }

  protected open fun init() {
  }

  protected open fun preExec(workingDir: Path) {
  }

  override fun exec() {
    preExec()
    logger.lifecycle("Starting {}...", displayName.get())
    logger.lifecycle("")
    super.exec()
  }

  private fun preExec() {
    standardInput = System.`in`
    workingDir(runDirectory)

    val selectedClasspath = if (!runClasspath.isEmpty) {
      runClasspath
    } else {
      if (!version.isPresent) {
        error("'runClasspath' is empty and no version was specified for the '$name' task. Don't know what version to download.")
      }
      downloadsApiService.get().resolveBuild(
        project,
        version.get(),
        build.get()
      )
    }
    classpath(selectedClasspath)

    // Create working dir if needed
    val workingDir = runDirectory.path
    if (!workingDir.isDirectory()) {
      workingDir.createDirectories()
    }

    preExec(workingDir)
  }

  /**
   * Convenience method for configuring [runClasspath] with a single jar.
   *
   * @param file jar file
   */
  public fun runJar(file: File) {
    runClasspath.from(file)
  }

  /**
   * Convenience method for configuring [runClasspath] with a single jar.
   *
   * @param file jar file provider
   */
  public fun runJar(file: Provider<RegularFile>) {
    runClasspath.from(file)
  }

  /**
   * Convenience method to set the [version] property.
   *
   * @param version version
   */
  public fun version(version: String) {
    this.version.set(version)
  }

  /**
   * Convenience method to set the [build] property.
   *
   * @param build build
   */
  public fun build(build: DownloadsAPIService.Build) {
    this.build.set(build)
  }

  /**
   * Convenience method to set [build] to a specific build number.
   *
   * @param buildNumber build number
   */
  public fun build(buildNumber: Int) {
    build.set(DownloadsAPIService.Build.Specific(buildNumber))
  }

  /**
   * Convenience method to set the [runDirectory] property.
   *
   * @param directory run directory
   */
  public fun runDirectory(directory: File) {
    runDirectory.set(directory)
  }
}
