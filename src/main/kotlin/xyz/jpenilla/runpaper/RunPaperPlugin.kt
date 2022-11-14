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
package xyz.jpenilla.runpaper

import io.papermc.paperweight.tasks.RemapJar
import io.papermc.paperweight.userdev.PaperweightUserExtension
import io.papermc.paperweight.util.constants.MOJANG_MAPPED_SERVER_RUNTIME_CONFIG
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import xyz.jpenilla.runpaper.task.RunServer
import xyz.jpenilla.runtask.RunExtension
import xyz.jpenilla.runtask.RunPlugin
import xyz.jpenilla.runtask.service.DownloadsAPIService
import xyz.jpenilla.runtask.util.Constants
import xyz.jpenilla.runtask.util.sharedCaches

public abstract class RunPaperPlugin : RunPlugin() {
  override fun apply(target: Project) {
    super.apply(target)

    val runExtension = target.extensions.create<RunExtension>(Constants.Extensions.RUN_PAPER, target)
    DownloadsAPIService.paper(target)

    target.tasks.register<Delete>(Constants.Tasks.CLEAN_PAPERCLIP_CACHE) {
      group = Constants.RUN_PAPER_TASK_GROUP
      description = "Delete all locally cached Paper jars."
      delete(target.sharedCaches.resolve(Constants.PAPER_PATH))
    }

    val runServer = target.tasks.register<RunServer>(Constants.Tasks.RUN_SERVER) {
      group = Constants.RUN_PAPER_TASK_GROUP
      description = "Run a Paper server for plugin testing."
    }
    runServer.setupPluginJarDetection(target, runExtension)

    target.plugins.withId(Constants.Plugins.PAPERWEIGHT_USERDEV_PLUGIN_ID) {
      target.setupPaperweightCompat(runServer, runExtension)
    }
  }

  override fun findPluginJar(project: Project): Provider<RegularFile>? = when {
    project.plugins.hasPlugin(Constants.Plugins.PAPERWEIGHT_USERDEV_PLUGIN_ID) -> {
      project.tasks.named<RemapJar>(Constants.Plugins.PAPERWEIGHT_REOBF_JAR_TASK_NAME).flatMap { it.outputJar }
    }

    else -> super.findPluginJar(project)
  }

  private fun Project.setupPaperweightCompat(
    runServer: TaskProvider<RunServer>,
    runExtension: RunExtension
  ) {
    val paperweight = the<PaperweightUserExtension>()

    runServer {
      version.convention(paperweight.minecraftVersion)
    }

    val runMojangMappedServer = tasks.register<RunServer>(Constants.Tasks.RUN_MOJANG_MAPPED_SERVER) {
      group = Constants.RUN_PAPER_TASK_GROUP
      description = "Run a Mojang mapped Paper server for plugin testing, by integrating with paperweight."
      version.value(paperweight.minecraftVersion).disallowChanges()

      val serverRuntimeConfig = configurations.findByName(MOJANG_MAPPED_SERVER_RUNTIME_CONFIG)
      if (serverRuntimeConfig == null) {
        val legacyMethod = PaperweightUserExtension::class.java.declaredMethods.find {
          it.name == "getMojangMappedServerJar" && it.returnType == Provider::class.java
        } ?: error("Could not find getMojangMappedServerJar on PaperweightUserExtension")

        @Suppress("unchecked_cast")
        val mojangMappedServerJarProvider = legacyMethod(paperweight) as Provider<RegularFile>
        runClasspath.from(mojangMappedServerJarProvider).disallowChanges()
      } else {
        mainClass.value("org.bukkit.craftbukkit.Main").disallowChanges()
        runClasspath.from(serverRuntimeConfig).disallowChanges()
      }
    }

    afterEvaluate {
      runMojangMappedServer {
        if (runExtension.detectPluginJar.get()) {
          pluginJars(tasks.named<RemapJar>(Constants.Plugins.PAPERWEIGHT_REOBF_JAR_TASK_NAME).flatMap { it.inputJar })
        }
      }
    }
  }
}
