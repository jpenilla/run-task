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
package xyz.jpenilla.runpaper

import io.papermc.paperweight.tasks.RemapJar
import io.papermc.paperweight.userdev.PaperweightUserExtension
import io.papermc.paperweight.util.constants.MOJANG_MAPPED_SERVER_RUNTIME_CONFIG
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import xyz.jpenilla.runpaper.task.RunServer
import xyz.jpenilla.runtask.RunExtension
import xyz.jpenilla.runtask.RunPlugin
import xyz.jpenilla.runtask.service.DownloadsAPIService
import xyz.jpenilla.runtask.util.Constants
import xyz.jpenilla.runtask.util.sharedCaches

public abstract class RunPaperPlugin : RunPlugin() {
  override fun apply(target: Project) {
    super.apply(target)

    val runExtension = target.extensions.create<RunPaperExtension>(Constants.Extensions.RUN_PAPER, target)
    DownloadsAPIService.paper(target)

    target.tasks.register<Delete>(Constants.Tasks.CLEAN_PAPER_CACHE) {
      group = Constants.RUN_PAPER_TASK_GROUP
      description = "Delete all locally cached Paper jars."
      delete(target.sharedCaches.resolve(Constants.PAPER_PATH))
    }

    target.tasks.register<Delete>(Constants.Tasks.CLEAN_PAPER_PLUGINS_CACHE) {
      group = Constants.RUN_PAPER_TASK_GROUP
      description = "Delete all locally cached Paper plugin jars."
      delete(target.sharedCaches.resolve(Constants.PAPER_PLUGINS_PATH))
    }

    val runServer = target.tasks.register<RunServer>(Constants.Tasks.RUN_SERVER) {
      group = Constants.RUN_PAPER_TASK_GROUP
      description = "Run a Paper server for plugin testing."
    }
    runServer.setupPluginJarDetection(target, runExtension)

    target.plugins.withId(Constants.Plugins.PAPERWEIGHT_USERDEV_PLUGIN_ID) {
      target.setupPaperweightCompat(runServer, runExtension)
    }

    target.afterEvaluate {
      val task = try {
        runExtension.folia.task()
      } catch (ex: UnknownTaskException) {
        return@afterEvaluate
      }
      when (runExtension.folia.pluginsMode.get()) {
        RunPaperExtension.Folia.PluginsMode.INHERIT_ALL -> {
          task.configure {
            pluginJars.from(runServer.map { it.pluginJars })
          }
        }
        RunPaperExtension.Folia.PluginsMode.INHERIT_NONE -> {}
        RunPaperExtension.Folia.PluginsMode.PLUGIN_JAR_DETECTION -> task.setupPluginJarDetection(this, runExtension, false)
      }
      task.configure {
        version.convention(runServer.flatMap { it.version })
      }
    }
  }

  override fun findPluginJar(project: Project): Provider<RegularFile>? = when {
    project.plugins.hasPlugin(Constants.Plugins.PAPERWEIGHT_USERDEV_PLUGIN_ID) -> {
      val paperweight = project.extensions.getByType<PaperweightUserExtension>()
      if (paperweight.minecraftVersion.get().minecraftVersionIsSameOrNewerThan(1, 20, 5)) {
        super.findPluginJar(project)
      } else {
        project.tasks.named<RemapJar>(Constants.Plugins.PAPERWEIGHT_REOBF_JAR_TASK_NAME).flatMap { it.outputJar }
      }
    }

    else -> super.findPluginJar(project)
  }

  private fun Project.setupPaperweightCompat(
    runServer: TaskProvider<RunServer>,
    runExtension: RunExtension
  ) {
    val paperweight = extensions.getByType<PaperweightUserExtension>()

    runServer {
      version.convention(paperweight.minecraftVersion)
    }

    val runTask = registerDevBundleRun(Constants.Tasks.RUN_DEV_BUNDLE_SERVER) {
      group = Constants.RUN_PAPER_TASK_GROUP
      description = "Run a Mojang mapped Paper server for plugin testing, by integrating with paperweight."
    }
    val deprecatedRunTask = registerDevBundleRun(Constants.Tasks.RUN_MOJANG_MAPPED_SERVER) {
      description = "Deprecated equivalent of ${Constants.Tasks.RUN_DEV_BUNDLE_SERVER}"
    }

    afterEvaluate {
      val op = Action<RunServer> {
        if (runExtension.detectPluginJar.get()) {
          pluginJars(tasks.named<RemapJar>(Constants.Plugins.PAPERWEIGHT_REOBF_JAR_TASK_NAME).flatMap { it.inputJar })
        }
      }
      runTask.configure(op)
      deprecatedRunTask.configure(op)
    }
  }

  private fun Project.registerDevBundleRun(name: String, op: Action<RunServer>): TaskProvider<RunServer> {
    val paperweight = extensions.getByType<PaperweightUserExtension>()

    return tasks.register<RunServer>(name) {
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

      op.execute(this)
    }
  }
}
