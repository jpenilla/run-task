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
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerIfAbsent
import xyz.jpenilla.runpaper.service.PaperclipService
import xyz.jpenilla.runpaper.task.RunServerTask
import java.io.File

public class RunPaper : Plugin<Project> {
  override fun apply(target: Project) {
    target.extensions.create<RunPaperExtension>(Constants.Extensions.RUN_PAPER, target)

    target.gradle.sharedServices.registerIfAbsent(Constants.Services.PAPERCLIP, PaperclipService::class) {
      this.maxParallelUsages.set(1)
      this.parameters.cacheDirectory.set(this@RunPaper.resolveSharedCachesDirectory(target))
    }

    val runServer = target.tasks.register<RunServerTask>(Constants.Tasks.RUN_SERVER) {
      this.group = Constants.RUN_PAPER
      this.description = "Run a Paper server for plugin testing."
    }
    target.afterEvaluate {
      runServer.configure {
        // Try to find plugin jar & task dependency automatically
        val taskDependency = this.resolveTaskDependency()
        if (taskDependency != null) {
          this.dependsOn(taskDependency)
          this.pluginJars.from(taskDependency.archiveFile)
        }
      }
    }

    target.tasks.register<Delete>("cleanPaperclipCache") {
      this.group = Constants.RUN_PAPER
      this.description = "Delete all locally cached Paperclips."
      this.delete(this@RunPaper.resolveSharedCachesDirectory(target))
    }
  }

  private fun resolveSharedCachesDirectory(project: Project): File {
    return project.gradle.gradleUserHomeDir.resolve(Constants.GRADLE_CACHES_DIRECTORY_NAME).resolve(Constants.RUN_PAPER).resolve("v1")
  }
}
