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
package xyz.jpenilla.runtask.util

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

internal fun Project.findJavaLauncher(): Provider<JavaLauncher>? {
  val service = project.extensions.findByType<JavaToolchainService>() ?: return null
  return project.extensions.findByType<JavaPluginExtension>()?.toolchain?.let { toolchain ->
    service.launcherFor(toolchain)
  }
}

internal inline fun <reified T : Task> TaskContainer.maybeRegister(
  taskName: String,
  noinline configuration: T.() -> Unit
): TaskProvider<T> = try {
  named<T>(taskName)
} catch (ex: UnknownTaskException) {
  register(taskName, configuration)
}
