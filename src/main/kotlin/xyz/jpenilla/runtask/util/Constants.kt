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

internal object Constants {
  const val SHARED_TASK_GROUP = "Run Task Shared"
  const val RUN_PAPER_TASK_GROUP = "Run Paper"
  const val RUN_VELOCITY_TASK_GROUP = "Run Velocity"
  const val RUN_WATERFALL_TASK_GROUP = "Run Waterfall"

  const val GRADLE_CACHES_DIRECTORY_NAME = "caches"
  const val RUN_PATH = "run-task-jars"
  const val USER_PATH = "$RUN_PATH/user"
  const val PAPER_PATH = "$RUN_PATH/paper"
  const val VELOCITY_PATH = "$RUN_PATH/velocity"
  const val WATERFALL_PATH = "$RUN_PATH/waterfall"

  object Plugins {
    const val SHADOW_PLUGIN_ID = "com.github.johnrengelman.shadow"
    const val SHADOW_JAR_TASK_NAME = "shadowJar"

    const val PAPERWEIGHT_USERDEV_PLUGIN_ID = "io.papermc.paperweight.userdev"
    const val PAPERWEIGHT_REOBF_JAR_TASK_NAME = "reobfJar"
  }

  object Tasks {
    const val CLEAN_PAPERCLIP_CACHE = "cleanPaperclipCache"
    const val CLEAN_VELOCITY_CACHE = "cleanVelocityCache"
    const val CLEAN_WATERFALL_CACHE = "cleanWaterfallCache"
    const val CLEAN_USER_SERVICES_CACHE = "cleanCustomServiceCaches"
    const val CLEAN_ALL_CACHES = "cleanAllRunTaskCaches"

    const val RUN_SERVER = "runServer"
    const val RUN_MOJANG_MAPPED_SERVER = "runMojangMappedServer"
    const val RUN_VELOCITY = "runVelocity"
    const val RUN_WATERFALL = "runWaterfall"
  }

  object Services {
    const val PAPER = "paper-download-service"
    const val VELOCITY = "velocity-download-service"
    const val WATERFALL = "waterfall-download-service"
  }

  object Properties {
    const val UPDATE_CHECK_FREQUENCY = "xyz.jpenilla.run-task.updateCheckFrequency"
    const val UPDATE_CHECK_FREQUENCY_LEGACY = "xyz.jpenilla.run-paper.updateCheckFrequency"
  }

  object Extensions {
    const val RUN_PAPER = "runPaper"

    // Extension suffix needed to avoid conflict with task name (Kotlin DSL prioritizes
    // extension even when in TaskContainer scope)...
    const val RUN_VELOCITY = "runVelocityExtension"
    const val RUN_WATERFALL = "runWaterfallExtension"
  }
}
