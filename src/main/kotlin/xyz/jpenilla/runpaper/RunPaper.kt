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
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerIfAbsent
import xyz.jpenilla.runpaper.service.PaperclipService
import xyz.jpenilla.runpaper.task.RunServerTask
import java.io.File

public class RunPaper : Plugin<Project> {
  override fun apply(target: Project) {
    val runPaperExtension = target.extensions.create<RunPaperExtension>(Constants.Extensions.RUN_PAPER, target)

    target.gradle.sharedServices.registerIfAbsent(Constants.Services.PAPERCLIP, PaperclipService::class) {
      this.maxParallelUsages.set(1)
      this.parameters.cacheDirectory.set(this@RunPaper.resolveSharedCachesDirectory(target))
      this.parameters.refreshDependencies.set(target.gradle.startParameter.isRefreshDependencies)
      this.parameters.offlineMode.set(target.gradle.startParameter.isOffline)
    }

    val runServer = target.tasks.register<RunServerTask>(Constants.Tasks.RUN_SERVER) {
      this.group = Constants.TASK_GROUP
      this.description = "Run a Paper server for plugin testing."
    }
    target.afterEvaluate {
      if (!runPaperExtension.detectPluginJar.get()) return@afterEvaluate

      runServer.configure {
        // Try to find plugin jar
        val pluginJarTask = target.pluginJar()
        if (pluginJarTask != null) {
          this.pluginJars(pluginJarTask)
        }
      }
    }

    target.tasks.register<Delete>("cleanPaperclipCache") {
      this.group = Constants.TASK_GROUP
      this.description = "Delete all locally cached Paperclips."
      this.delete(this@RunPaper.resolveSharedCachesDirectory(target))
    }
  }

  private fun resolveSharedCachesDirectory(project: Project): File {
    return project.gradle.gradleUserHomeDir.resolve(Constants.GRADLE_CACHES_DIRECTORY_NAME).resolve(Constants.RUN_PAPER).resolve("v1")
  }

  private fun Project.pluginJar(): Provider<RegularFile>? {
    when {
      this.plugins.hasPlugin(Constants.Plugins.PAPERWEIGHT_USERDEV_PLUGIN_ID) -> {
        val reobfJar = this.tasks.named(Constants.Plugins.PAPERWEIGHT_REOBF_JAR_TASK_NAME)
        return layout.file(reobfJar.map { it.outputs.files.singleFile })
      }
      this.plugins.hasPlugin(Constants.Plugins.SHADOW_PLUGIN_ID) -> {
        return this.tasks.named<AbstractArchiveTask>(Constants.Plugins.SHADOW_JAR_TASK_NAME).flatMap { it.archiveFile }
      }
      else -> {
        val jar = this.tasks.findByName(JavaPlugin.JAR_TASK_NAME) as? AbstractArchiveTask ?: return null
        return jar.archiveFile
      }
    }
  }
}
