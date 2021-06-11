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

internal object Constants {
  const val RUN_PAPER = "RunPaper"
  const val TASK_GROUP = "Run Paper"
  const val GRADLE_CACHES_DIRECTORY_NAME = "caches"

  object Plugins {
    const val SHADOW_PLUGIN_ID = "com.github.johnrengelman.shadow"
    const val SHADOW_JAR_TASK_NAME = "shadowJar"
  }

  object Tasks {
    const val RUN_SERVER = "runServer"
  }

  object Services {
    const val PAPERCLIP = "paperclip"
  }

  object Properties {
    private fun runPaper(propertyName: String) = "xyz.jpenilla.run-paper.$propertyName"

    val UPDATE_CHECK_FREQUENCY = this.runPaper("updateCheckFrequency")
  }

  object Extensions {
    const val RUN_PAPER = "runPaper"
  }
}
