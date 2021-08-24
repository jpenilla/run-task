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

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import xyz.jpenilla.runpaper.task.RunServerTask

public abstract class RunPaperExtension(project: Project) {
  internal val detectPluginJar: Property<Boolean> = project.objects.property<Boolean>().convention(true)

  /**
   * By default, Run Paper will attempt to discover your plugin `jar` or `shadowJar` and automatically add it to
   * the [RunServerTask.pluginJars] file collection. In some configurations, this behavior may not be desired,
   * and therefore this option exists to disable it.
   *
   * Note that the plugin jar discovery behavior is only applicable for the default `runServer` task created by
   * Run Paper, if you create your own [RunServerTask]s you will need to manually add plugin jars regardless.
   */
  public fun disablePluginJarDetection() {
    detectPluginJar.set(false)
  }
}
