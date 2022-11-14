/*
 * Run Task Gradle Plugins
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
package xyz.jpenilla.runvelocity.task

import xyz.jpenilla.runtask.service.DownloadsAPIService
import xyz.jpenilla.runtask.task.AbstractRun
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * Task to download and run a Velocity server along with plugins.
 */
public abstract class RunVelocity : AbstractRun() {
  override fun init() {
    downloadsApiService.convention(DownloadsAPIService.velocity(project))
    displayName.convention("Velocity")
  }

  override fun preExec(workingDir: Path) {
    setupPlugins(workingDir)
  }

  private fun setupPlugins(workingDir: Path) {
    val plugins = workingDir.resolve("plugins")
    if (!plugins.isDirectory()) {
      plugins.createDirectories()
    }

    val suffix = "_run-velocity_plugin.jar"

    // Delete any jars left over from previous runs
    plugins.listDirectoryEntries()
      .filter { it.isRegularFile() && it.name.endsWith(suffix) }
      .forEach { it.deleteIfExists() }

    // Add plugins
    pluginJars.files.map { it.toPath() }.forEach { jar ->
      val name = jar.fileName.toString()
      jar.copyTo(plugins.resolve(name.substring(0, name.length - 4 /*.jar*/) + suffix))
    }
  }

  /**
   * Sets the Velocity version to use.
   *
   * Convenience method to set the [version] property.
   *
   * @param velocityVersion Velocity version
   */
  public fun velocityVersion(velocityVersion: String) {
    version(velocityVersion)
  }
}
