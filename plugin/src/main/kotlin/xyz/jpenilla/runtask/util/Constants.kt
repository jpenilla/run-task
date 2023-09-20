/*
 * Run Task Gradle Plugins
 * Copyright (c) 2023 Jason Penilla
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
  const val FOLIA_PATH = "$RUN_PATH/folia"
  const val VELOCITY_PATH = "$RUN_PATH/velocity"
  const val WATERFALL_PATH = "$RUN_PATH/waterfall"

  const val PAPER_PLUGINS_PATH = "$RUN_PATH/plugins/paper"
  const val VELOCITY_PLUGINS_PATH = "$RUN_PATH/plugins/velocity"
  const val WATERFALL_PLUGINS_PATH = "$RUN_PATH/plugins/waterfall"

  const val HANGAR_PLUGIN_DIR = "hangar"
  const val MODRINTH_PLUGIN_DIR = "modrinth"
  const val GITHUB_PLUGIN_DIR = "github"
  const val URL_PLUGIN_DIR = "url"

  object Plugins {
    const val SHADOW_PLUGIN_ID = "com.github.johnrengelman.shadow"
    const val SHADOW_JAR_TASK_NAME = "shadowJar"

    const val PAPERWEIGHT_USERDEV_PLUGIN_ID = "io.papermc.paperweight.userdev"
    const val PAPERWEIGHT_REOBF_JAR_TASK_NAME = "reobfJar"
  }

  object Tasks {
    const val CLEAN_PAPER_CACHE = "cleanPaperCache"
    const val CLEAN_FOLIA_CACHE = "cleanFoliaCache"
    const val CLEAN_VELOCITY_CACHE = "cleanVelocityCache"
    const val CLEAN_WATERFALL_CACHE = "cleanWaterfallCache"
    const val CLEAN_USER_SERVICES_CACHE = "cleanCustomServiceCaches"
    // const val CLEAN_RUNTIME_JARS = "cleanRuntimeJars"

    const val CLEAN_PAPER_PLUGINS_CACHE = "cleanPaperPluginsCache"
    const val CLEAN_VELOCITY_PLUGINS_CACHE = "cleanVelocityPluginsCache"
    const val CLEAN_WATERFALL_PLUGINS_CACHE = "cleanWaterfallPluginsCache"
    // const val CLEAN_PLUGIN_JARS = "cleanPluginJars"

    const val CLEAN_ALL_CACHES = "cleanAllRunTaskCaches"

    const val RUN_SERVER = "runServer"
    const val RUN_FOLIA = "runFolia"
    const val RUN_MOJANG_MAPPED_SERVER = "runMojangMappedServer"
    const val RUN_VELOCITY = "runVelocity"
    const val RUN_WATERFALL = "runWaterfall"
  }

  object Services {
    const val PAPER = "paper-download-service"
    const val FOLIA = "folia-download-service"
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
