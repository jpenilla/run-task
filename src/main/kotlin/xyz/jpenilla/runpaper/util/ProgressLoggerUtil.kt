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
package xyz.jpenilla.runpaper.util

import org.gradle.api.Project
import java.lang.reflect.Method

internal object ProgressLoggerUtil {
  fun createProgressLogger(project: Project, operationName: String): ProgressLoggerWrapper? = try {
    val serviceFactory = project.javaClass.getMethod("getServices").invoke(project)
    val get = serviceFactory.javaClass.getMethod("get", Class::class.java)
    val progressLoggerFactoryClass = Class.forName("org.gradle.internal.logging.progress.ProgressLoggerFactory")
    val factory = get(serviceFactory, progressLoggerFactoryClass)
    val newOperation = progressLoggerFactoryClass.getMethod("newOperation", String::class.java)
    val progressLoggerClass = Class.forName("org.gradle.internal.logging.progress.ProgressLogger")
    val start = progressLoggerClass.getMethod("start", String::class.java, String::class.java)
    val progress = progressLoggerClass.getMethod("progress", String::class.java)
    val completed = progressLoggerClass.getMethod("completed")
    ProgressLoggerWrapper(newOperation(factory, operationName), start, progress, completed)
  } catch (ex: ReflectiveOperationException) {
    null
  }

  class ProgressLoggerWrapper(
    private val logger: Any,
    private val start: Method,
    private val progress: Method,
    private val completed: Method
  ) {
    fun start(description: String, status: String) {
      start(logger, description, status)
    }

    fun progress(status: String) {
      progress(logger, status)
    }

    fun completed() {
      completed(logger)
    }
  }
}
