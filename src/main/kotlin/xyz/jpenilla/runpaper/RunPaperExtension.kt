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
package xyz.jpenilla.runpaper

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import xyz.jpenilla.runpaper.task.RunServer
import xyz.jpenilla.runtask.RunExtension
import xyz.jpenilla.runtask.service.DownloadsAPIService
import xyz.jpenilla.runtask.task.RunWithPlugins
import xyz.jpenilla.runtask.util.Constants
import xyz.jpenilla.runtask.util.maybeRegister
import javax.inject.Inject

/**
 * `run-paper` specific extension of [RunExtension].
 */
public abstract class RunPaperExtension(project: Project) : RunExtension(project.objects) {
  public val folia: Folia = project.objects.newInstance(Folia::class, project)

  /**
   * Configures [folia] with [op].
   *
   * @param op configuration action
   */
  public fun folia(op: Action<Folia>): Unit = op.execute(folia)

  /**
   * Holds configuration for the optional built in `runFolia` task.
   */
  public abstract class Folia @Inject constructor(private val project: Project) {
    /**
     * Sets how the `runFolia` task's [RunServer.pluginJars] collection will be handled, if
     * the task is registered with [registerTask].
     *
     * Defaults to [PluginsMode.PLUGIN_JAR_DETECTION]
     */
    public val pluginsMode: Property<PluginsMode> = project.objects.property<PluginsMode>()
      .convention(PluginsMode.PLUGIN_JAR_DETECTION)

    /**
     * Registers a [RunServer] task for Folia, named `runFolia`.
     *
     * The task will inherit it's Minecraft version from `runServer` by default.
     * Behavior for the task's [RunServer.pluginJars] collection can be modified through the [pluginsMode] property.
     *
     * @param op configuration action
     * @return task provider
     */
    @JvmOverloads
    public fun registerTask(op: Action<RunServer> = Action {}): TaskProvider<RunServer> = project.tasks.maybeRegister(Constants.Tasks.RUN_FOLIA) {
      group = Constants.RUN_PAPER_TASK_GROUP
      description = "Run a Folia server for plugin testing."
      displayName.set("Folia")
      downloadsApiService.convention(DownloadsAPIService.folia(project))
      op.execute(this)
    }

    /**
     * Gets the registered `runFolia` task, throwing [UnknownTaskException] if it has
     * not been registered with [registerTask]. Also configures it with [op].
     *
     * @param op configuration action
     * @return task provider
     */
    @JvmOverloads
    public fun task(op: Action<RunServer> = Action {}): TaskProvider<RunServer> = project.tasks.named<RunServer>(Constants.Tasks.RUN_FOLIA) {
      op.execute(this)
    }

    public enum class PluginsMode {
      /**
       * In this mode, `runFolia`s [RunWithPlugins.pluginJars] collection
       * inherit everything from `runServer's, including any auto-detected plugin
       * jars (see [RunExtension.detectPluginJar]).
       */
      INHERIT_ALL,

      /**
       * In this mode, `runFolia`s [RunWithPlugins.pluginJars] collection will be left
       * empty, for maximum flexibility.
       */
      INHERIT_NONE,

      /**
       * In this mode, `runFolia` will not inherit anything from `runServer`s [RunWithPlugins.pluginJars] collection,
       * similar to [INHERIT_NONE].
       *
       * However, this mode is different from [INHERIT_NONE] in that the default plugin jar detection
       * described at [RunExtension.detectPluginJar] will be done for `runFolia` (if enabled).
       *
       * This is the default mode.
       */
      PLUGIN_JAR_DETECTION
    }
  }
}
