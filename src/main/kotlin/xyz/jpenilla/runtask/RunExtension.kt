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
package xyz.jpenilla.runtask

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.polymorphicDomainObjectContainer
import org.gradle.kotlin.dsl.property
import xyz.jpenilla.runtask.pluginsapi.DownloadPluginsSpec
import xyz.jpenilla.runtask.pluginsapi.PluginApi
import xyz.jpenilla.runtask.task.AbstractRun
import javax.inject.Inject

public abstract class RunExtension @Inject constructor(private val objects: ObjectFactory) {
  /**
   * By default, Run Paper/Velocity/Waterfall will attempt to discover your plugin `jar` or `shadowJar` and automatically
   * add it to the [AbstractRun.pluginJars] file collection. In some configurations, this behavior may not be desired,
   * and therefore this option exists to disable it.
   *
   * Note that the plugin jar discovery behavior is only applicable for the automatically registered default run
   * task, if you create your own [AbstractRun]s you will need to manually add plugin jars regardless.
   */
  public val detectPluginJar: Property<Boolean> = objects.property<Boolean>().convention(true)

  /**
   * Configures [detectPluginJar] to false.
   */
  public fun disablePluginJarDetection() {
    detectPluginJar.set(false)
  }

  public fun downloadPluginsSpec(): DownloadPluginsSpec {
    return objects.newInstance(DownloadPluginsSpec::class, objects.polymorphicDomainObjectContainer(PluginApi::class))
  }

  public fun downloadPluginsSpec(config: Action<in DownloadPluginsSpec>): DownloadPluginsSpec {
    val spec = downloadPluginsSpec()
    config.execute(spec)
    return spec
  }
}
