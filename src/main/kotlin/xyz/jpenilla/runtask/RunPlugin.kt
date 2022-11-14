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
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import xyz.jpenilla.runtask.task.AbstractRun
import xyz.jpenilla.runtask.util.Constants
import xyz.jpenilla.runtask.util.find
import xyz.jpenilla.runtask.util.findJavaLauncher
import xyz.jpenilla.runtask.util.maybeRegister
import xyz.jpenilla.runtask.util.sharedCaches

public abstract class RunPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.registerCleanUserCachesTask()
    javaLauncherConvention(target)
  }

  private fun javaLauncherConvention(target: Project) {
    target.plugins.withId("java") {
      // Use the configured Java toolchain if present
      target.findJavaLauncher()?.let { launcher ->
        target.tasks.withType<AbstractRun>().configureEach {
          javaLauncher.convention(launcher)
        }
      }
    }
  }

  protected fun Project.setupPluginJarDetection(
    task: TaskProvider<out AbstractRun>,
    extension: RunExtension
  ) {
    afterEvaluate {
      if (extension.detectPluginJar.get()) {
        val jar = findPluginJar(this)
        task { jar?.let { pluginJars(it) } }
      }
    }
  }

  private fun Project.registerCleanUserCachesTask() {
    tasks.maybeRegister<Delete>(Constants.Tasks.CLEAN_USER_SERVICES_CACHE) {
      group = Constants.SHARED_TASK_GROUP
      description = "Delete all locally cached jars for custom downloads API service registrations."
      delete(sharedCaches.resolve(Constants.USER_PATH))
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
