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
package xyz.jpenilla.runtask

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import xyz.jpenilla.runtask.task.AbstractRun
import xyz.jpenilla.runtask.task.RunWithPlugins
import xyz.jpenilla.runtask.util.Constants
import xyz.jpenilla.runtask.util.findJavaLauncher
import xyz.jpenilla.runtask.util.maybeRegister
import xyz.jpenilla.runtask.util.sharedCaches

public abstract class RunPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.registerSharedCleanTasks()
    javaLauncherConvention(target)
  }

  private fun javaLauncherConvention(target: Project) {
    target.afterEvaluate {
      // Use the configured Java toolchain if present
      target.findJavaLauncher()?.let { launcher ->
        target.tasks.withType<AbstractRun>().configureEach {
          javaLauncher.convention(launcher)
        }
      }
    }
  }

  protected fun TaskProvider<out RunWithPlugins>.setupPluginJarDetection(
    project: Project,
    extension: RunExtension,
    scheduleForAfterEvaluate: Boolean = true
  ) {
    val action = {
      if (extension.detectPluginJar.get()) {
        findPluginJar(project)?.let {
          configure { pluginJars(it) }
        }
      }
    }
    if (scheduleForAfterEvaluate) {
      project.afterEvaluate { action() }
    } else {
      action()
    }
  }

  private fun Project.registerSharedCleanTasks() {
    tasks.maybeRegister<Delete>(Constants.Tasks.CLEAN_USER_SERVICES_CACHE) {
      group = Constants.SHARED_TASK_GROUP
      description = "Delete all locally cached jars for custom downloads API service registrations."
      delete(sharedCaches.resolve(Constants.USER_PATH))
    }
    tasks.maybeRegister<Delete>(Constants.Tasks.CLEAN_ALL_CACHES) {
      group = Constants.SHARED_TASK_GROUP
      description = "Delete all locally cached jars for run tasks. " +
        "Roughly equivalent to running '${Constants.Tasks.CLEAN_USER_SERVICES_CACHE} ${Constants.Tasks.CLEAN_PAPERCLIP_CACHE} ${Constants.Tasks.CLEAN_VELOCITY_CACHE} ${Constants.Tasks.CLEAN_WATERFALL_CACHE}'."
      delete(sharedCaches.resolve(Constants.RUN_PATH))
    }
  }

  protected open fun findPluginJar(project: Project): Provider<RegularFile>? = when {
    project.plugins.hasPlugin(Constants.Plugins.SHADOW_PLUGIN_ID) -> {
      project.tasks.named<AbstractArchiveTask>(Constants.Plugins.SHADOW_JAR_TASK_NAME).flatMap { it.archiveFile }
    }

    else -> {
      try {
        project.tasks.named<AbstractArchiveTask>(JavaPlugin.JAR_TASK_NAME).flatMap { it.archiveFile }
      } catch (ex: UnknownTaskException) {
        null
      }
    }
  }
}
