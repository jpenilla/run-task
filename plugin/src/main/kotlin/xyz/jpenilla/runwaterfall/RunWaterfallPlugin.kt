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
package xyz.jpenilla.runwaterfall

import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import xyz.jpenilla.runtask.RunExtension
import xyz.jpenilla.runtask.RunPlugin
import xyz.jpenilla.runtask.service.DownloadsAPIService
import xyz.jpenilla.runtask.util.Constants
import xyz.jpenilla.runtask.util.sharedCaches
import xyz.jpenilla.runwaterfall.task.RunWaterfall

public abstract class RunWaterfallPlugin : RunPlugin() {
  override fun apply(target: Project) {
    super.apply(target)

    val runExtension = target.extensions.create<RunExtension>(Constants.Extensions.RUN_WATERFALL)
    DownloadsAPIService.waterfall(target)

    target.tasks.register<Delete>(Constants.Tasks.CLEAN_WATERFALL_CACHE) {
      group = Constants.RUN_WATERFALL_TASK_GROUP
      description = "Delete all locally cached Waterfall jars."
      delete(target.sharedCaches.resolve(Constants.WATERFALL_PATH))
    }

    target.tasks.register<Delete>(Constants.Tasks.CLEAN_WATERFALL_PLUGINS_CACHE) {
      group = Constants.RUN_WATERFALL_TASK_GROUP
      description = "Delete all locally cached Waterfall plugin jars."
      delete(target.sharedCaches.resolve(Constants.WATERFALL_PLUGINS_PATH))
    }

    val runWaterfall = target.tasks.register<RunWaterfall>(Constants.Tasks.RUN_WATERFALL) {
      group = Constants.RUN_WATERFALL_TASK_GROUP
      description = "Run a Waterfall server for plugin testing."
    }
    runWaterfall.setupPluginJarDetection(target, runExtension)
  }
}
