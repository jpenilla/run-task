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

import io.papermc.paperweight.tasks.RemapJar
import io.papermc.paperweight.userdev.PaperweightUserExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.the
import xyz.jpenilla.runpaper.service.PaperclipService
import xyz.jpenilla.runpaper.task.RunServerTask
import xyz.jpenilla.runpaper.util.find
import xyz.jpenilla.runpaper.util.set
import xyz.jpenilla.runpaper.util.sharedCaches

public class RunPaper : Plugin<Project> {
  override fun apply(target: Project) {
    val runPaperExtension = target.extensions.create<RunPaperExtension>(Constants.Extensions.RUN_PAPER, target)

    target.gradle.sharedServices.registerIfAbsent(Constants.Services.PAPERCLIP, PaperclipService::class) {
      maxParallelUsages.set(1)
      parameters.cacheDirectory.set(target.sharedCaches.resolve(Constants.RUN_PAPER_PATH))
      parameters.refreshDependencies.set(target.gradle.startParameter.isRefreshDependencies)
      parameters.offlineMode.set(target.gradle.startParameter.isOffline)
    }

    target.tasks.register<Delete>(Constants.Tasks.CLEAN_PAPERCLIP_CACHE) {
      group = Constants.TASK_GROUP
      description = "Delete all locally cached Paperclips."
      delete(target.sharedCaches.resolve(Constants.RUN_PAPER_PATH))
    }

    val runServer = target.tasks.register<RunServerTask>(Constants.Tasks.RUN_SERVER) {
      group = Constants.TASK_GROUP
      description = "Run a Paper server for plugin testing."
    }
    target.afterEvaluate {
      if (!runPaperExtension.detectPluginJar.get()) return@afterEvaluate

      runServer {
        target.findPluginJar()?.let { pluginJar ->
          pluginJars(pluginJar)
        }
      }
    }

    target.plugins.withId(Constants.Plugins.PAPERWEIGHT_USERDEV_PLUGIN_ID) {
      target.setupPaperweightCompat(runServer, runPaperExtension)
    }
  }

  private fun Project.findPluginJar(): Provider<RegularFile>? = when {
    plugins.hasPlugin(Constants.Plugins.PAPERWEIGHT_USERDEV_PLUGIN_ID) -> {
      tasks.named<RemapJar>(Constants.Plugins.PAPERWEIGHT_REOBF_JAR_TASK_NAME).flatMap { it.outputJar }
    }
    plugins.hasPlugin(Constants.Plugins.SHADOW_PLUGIN_ID) -> {
      tasks.named<AbstractArchiveTask>(Constants.Plugins.SHADOW_JAR_TASK_NAME).flatMap { it.archiveFile }
    }
    else -> {
      tasks.find<AbstractArchiveTask>(JavaPlugin.JAR_TASK_NAME)?.archiveFile
    }
  }

  private fun Project.setupPaperweightCompat(
    runServer: TaskProvider<RunServerTask>,
    runPaperExtension: RunPaperExtension
  ) {
    val paperweight = the<PaperweightUserExtension>()

    runServer {
      minecraftVersion.convention(paperweight.minecraftVersion)
    }

    tasks.register<RunServerTask>(Constants.Tasks.RUN_MOJANG_MAPPED_SERVER) {
      group = Constants.TASK_GROUP
      description = "Run a Mojang mapped Paper server for plugin testing, by integrating with paperweight."
      serverJar.value(paperweight.mojangMappedServerJar).disallowChanges()
      minecraftVersion.value(paperweight.minecraftVersion).disallowChanges()
      if (runPaperExtension.detectPluginJar.get()) {
        pluginJars(tasks.named<RemapJar>(Constants.Plugins.PAPERWEIGHT_REOBF_JAR_TASK_NAME).flatMap { it.inputJar })
      }
    }
  }
}
