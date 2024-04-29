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
package xyz.jpenilla.runtask.task

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.polymorphicDomainObjectContainer
import xyz.jpenilla.runtask.RunExtension
import xyz.jpenilla.runtask.pluginsapi.DownloadPluginsSpec
import xyz.jpenilla.runtask.pluginsapi.PluginApi
import xyz.jpenilla.runtask.pluginsapi.PluginDownloadService
import java.nio.file.Path
import javax.inject.Inject

/**
 * Base task for runs which load a collection of plugin jars.
 *
 * Note that this class does not actually do anything to load
 * the plugins in [pluginJars], it's expected that subclasses
 * will implement this behavior in [preExec].
 */
public abstract class RunWithPlugins : AbstractRun() {

  /**
   * The collection of plugin jars to load.
   *
   * @see [RunExtension.detectPluginJar]
   */
  @get:Classpath
  public abstract val pluginJars: ConfigurableFileCollection

  /**
   * The spec of plugins to download and load in addition to [pluginJars].
   */
  @get:Nested
  public lateinit var downloadPlugins: DownloadPluginsSpec
    private set

  /**
   * The service used to download plugins in the [downloadPlugins] spec.
   */
  @get:Internal
  public abstract val pluginDownloadService: Property<PluginDownloadService>

  @get:Inject
  protected abstract val objects: ObjectFactory

  // not tracked by inputs/outputs so won't be finalized automatically like `pluginJars` will
  @get:Internal
  protected lateinit var ourPluginJars: ConfigurableFileCollection
    private set

  override fun init() {
    super.init()

    downloadPlugins = objects.newInstance(DownloadPluginsSpec::class, objects.polymorphicDomainObjectContainer(PluginApi::class))
    ourPluginJars = objects.fileCollection()
  }

  override fun preExec(workingDir: Path) {
    super.preExec(workingDir)

    ourPluginJars.from(pluginJars)

    val service = pluginDownloadService.get()
    for (download in downloadPlugins.downloads) {
      ourPluginJars.from(service.resolvePlugin(project, download))
    }
  }

  /**
   * Convenience method for easily adding jars to [pluginJars].
   *
   * @param jars jars to add
   */
  public fun pluginJars(vararg jars: Any) {
    pluginJars.from(jars)
  }

  /**
   * Convenience method for easily adding jars to [pluginJars].
   *
   * @param jars jars to add
   */
  public fun pluginJars(vararg jars: Provider<RegularFile>) {
    pluginJars.from(jars)
  }

  /**
   * Configure [downloadPlugins] with the provided action.
   *
   * @param config configuration action
   */
  public fun downloadPlugins(config: Action<DownloadPluginsSpec>) {
    config.execute(downloadPlugins)
  }

  // For groovy
  /**
   * Configure [downloadPlugins] with the provided action.
   *
   * @param config configuration action
   */
  public fun downloadPlugins(config: Closure<DownloadPluginsSpec>) {
    config.delegate = downloadPlugins
    config.run()
  }
}
