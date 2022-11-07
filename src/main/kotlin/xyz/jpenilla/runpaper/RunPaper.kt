/*
 * Run Paper Gradle Plugin
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
package xyz.jpenilla.runpaper

import io.papermc.paperweight.tasks.RemapJar
import io.papermc.paperweight.userdev.PaperweightUserExtension
import io.papermc.paperweight.util.constants.MOJANG_MAPPED_SERVER_RUNTIME_CONFIG
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
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import xyz.jpenilla.runpaper.paperapi.DownloadsAPI
import xyz.jpenilla.runpaper.paperapi.Projects
import xyz.jpenilla.runpaper.service.PaperclipService
import xyz.jpenilla.runpaper.task.RunServerTask
import xyz.jpenilla.runpaper.util.find
import xyz.jpenilla.runpaper.util.findJavaLauncher
import xyz.jpenilla.runpaper.util.sharedCaches

public class RunPaper : Plugin<Project> {
  override fun apply(target: Project) {
    val runPaperExtension = target.extensions.create<RunPaperExtension>(Constants.Extensions.RUN_PAPER, target)

    PaperclipService.register(target) {
      downloadsEndpoint = DownloadsAPI.PAPER_ENDPOINT
      downloadProjectName = Projects.PAPER
      buildServiceName = Constants.Services.PAPERCLIP
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

    target.plugins.withId(Constants.Plugins.PAPERWEIGHT_USERDEV_PLUGIN_ID) {
      target.setupPaperweightCompat(runServer, runPaperExtension)
    }

    target.afterEvaluate {
      if (runPaperExtension.detectPluginJar.get()) {
        runServer {
          findPluginJar()?.let { pluginJar ->
            pluginJars(pluginJar)
          }
        }
      }

      tasks.withType<RunServerTask> {
        // Use the configured Java toolchain if present
        findJavaLauncher()?.let { launcher ->
          javaLauncher.convention(launcher)
        }
      }
    }
  }

  private fun Project.findPluginJar(): Provider<RegularFile>? = when {
    plugins.hasPlugin(Constants.Plugins.PAPERWEIGHT_USERDEV_PLUGIN_ID) -> {
      tasks.named<RemapJar>(Constants.Plugins.PAPERWEIGHT_REOBF_JAR_TASK_NAME).flatMap { it.outputJar }
    }
    plugins.hasPlugin(Constants.Plugins.SHADOW_PLUGIN_ID) -> {
      tasks.named<AbstractArchiveTask>(Constants.Plugins.SHADOW_JAR_TASK_NAME).flatMap { it.archiveFile }
    }
    else -> tasks.find<AbstractArchiveTask>(JavaPlugin.JAR_TASK_NAME)?.archiveFile
  }

  private fun Project.setupPaperweightCompat(
    runServer: TaskProvider<RunServerTask>,
    runPaperExtension: RunPaperExtension
  ) {
    val paperweight = the<PaperweightUserExtension>()

    runServer {
      minecraftVersion.convention(paperweight.minecraftVersion)
    }

    val runMojangMappedServer = tasks.register<RunServerTask>(Constants.Tasks.RUN_MOJANG_MAPPED_SERVER) {
      group = Constants.TASK_GROUP
      description = "Run a Mojang mapped Paper server for plugin testing, by integrating with paperweight."
      minecraftVersion.value(paperweight.minecraftVersion).disallowChanges()

      val serverRuntimeConfig = configurations.findByName(MOJANG_MAPPED_SERVER_RUNTIME_CONFIG)
      if (serverRuntimeConfig == null) {
        val legacyMethod = PaperweightUserExtension::class.java.declaredMethods.find {
          it.name == "getMojangMappedServerJar" && it.returnType == Provider::class.java
        } ?: error("Could not find getMojangMappedServerJar on PaperweightUserExtension")

        @Suppress("unchecked_cast")
        val mojangMappedServerJarProvider = legacyMethod(paperweight) as Provider<RegularFile>
        serverJar.value(mojangMappedServerJarProvider).disallowChanges()
      } else {
        mainClass.value("org.bukkit.craftbukkit.Main").disallowChanges()
        serverClasspath.from(serverRuntimeConfig).disallowChanges()
      }
    }

    afterEvaluate {
      runMojangMappedServer {
        if (runPaperExtension.detectPluginJar.get()) {
          pluginJars(tasks.named<RemapJar>(Constants.Plugins.PAPERWEIGHT_REOBF_JAR_TASK_NAME).flatMap { it.inputJar })
        }
      }
    }
  }
}
