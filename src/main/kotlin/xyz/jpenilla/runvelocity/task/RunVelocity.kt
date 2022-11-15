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
import xyz.jpenilla.runtask.task.RunWithPlugins
import xyz.jpenilla.runtask.util.FileCopyingPluginHandler
import java.nio.file.Path

/**
 * Task to download and run a Velocity server along with plugins.
 */
public abstract class RunVelocity : RunWithPlugins() {
  override fun init() {
    downloadsApiService.convention(DownloadsAPIService.velocity(project))
    displayName.convention("Velocity")
  }

  override fun preExec(workingDir: Path) {
    FileCopyingPluginHandler("RunVelocity")
      .setupPlugins(workingDir.resolve("plugins"), pluginJars)
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
