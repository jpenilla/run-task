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
package xyz.jpenilla.runtask.task

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import xyz.jpenilla.runtask.RunExtension
import java.io.File

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
   * Convenience method for easily adding jars to [pluginJars].
   *
   * @param jars jars to add
   */
  public fun pluginJars(vararg jars: File) {
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
}
