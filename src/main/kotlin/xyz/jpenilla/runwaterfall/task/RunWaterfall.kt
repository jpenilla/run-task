/*
 * Run Task Gradle Plugins
 * Copyright (c) 2023 Jason Penilla
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
package xyz.jpenilla.runwaterfall.task

import xyz.jpenilla.runtask.service.DownloadsAPIService
import xyz.jpenilla.runtask.task.RunWithPlugins
import xyz.jpenilla.runtask.util.FileCopyingPluginHandler
import java.nio.file.Path

/**
 * Task to download and run a Waterfall server along with plugins.
 */
public abstract class RunWaterfall : RunWithPlugins() {
  override fun init() {
    downloadsApiService.convention(DownloadsAPIService.waterfall(project))
    displayName.convention("Waterfall")
  }

  override fun preExec(workingDir: Path) {
    FileCopyingPluginHandler("RunWaterfall")
      .setupPlugins(workingDir.resolve("plugins"), pluginJars)
  }

  /**
   * Sets the Waterfall version to use.
   *
   * Convenience method to set the [version] property.
   *
   * @param waterfallVersion Waterfall version
   */
  public fun waterfallVersion(waterfallVersion: String) {
    version(waterfallVersion)
  }
}
