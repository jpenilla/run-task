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
package xyz.jpenilla.runpaper

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import xyz.jpenilla.runpaper.task.RunServerTask

public class RunPaper : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply("de.undercouch.download")
    target.extensions.create<RunPaperExtension>("runPaper", target)
    val runServer = target.tasks.register<RunServerTask>("runServer")
    target.afterEvaluate {
      runServer.forUseAtConfigurationTime().get().apply {
        this.group = "RunPaper"
        this.description = "Run a Paper server for plugin testing."

        // Try to find plugin jar & task dependency automatically
        val taskDependency = resolveTaskDependency()
        if (taskDependency != null) {
          this.dependsOn(taskDependency)
          this.pluginJars.from(taskDependency.archiveFile)
        }
      }
    }
  }
}
