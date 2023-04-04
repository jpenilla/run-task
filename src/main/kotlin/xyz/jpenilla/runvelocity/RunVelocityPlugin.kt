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
package xyz.jpenilla.runvelocity

import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import xyz.jpenilla.runtask.RunExtension
import xyz.jpenilla.runtask.RunPlugin
import xyz.jpenilla.runtask.service.DownloadsAPIService
import xyz.jpenilla.runtask.util.Constants
import xyz.jpenilla.runtask.util.sharedCaches
import xyz.jpenilla.runvelocity.task.RunVelocity

public abstract class RunVelocityPlugin : RunPlugin() {
  override fun apply(target: Project) {
    super.apply(target)

    val runExtension = target.extensions.create<RunExtension>(Constants.Extensions.RUN_VELOCITY)
    DownloadsAPIService.velocity(target)

    target.tasks.register<Delete>(Constants.Tasks.CLEAN_VELOCITY_CACHE) {
      group = Constants.RUN_VELOCITY_TASK_GROUP
      description = "Delete all locally cached Velocity jars."
      delete(target.sharedCaches.resolve(Constants.VELOCITY_PATH))
    }

    val runVelocity = target.tasks.register<RunVelocity>(Constants.Tasks.RUN_VELOCITY) {
      group = Constants.RUN_VELOCITY_TASK_GROUP
      description = "Run a Velocity server for plugin testing."
    }
    runVelocity.setupPluginJarDetection(target, runExtension)
  }
}
