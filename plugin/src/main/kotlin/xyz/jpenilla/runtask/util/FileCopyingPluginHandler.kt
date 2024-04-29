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
package xyz.jpenilla.runtask.util

import org.gradle.api.file.FileCollection
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

public class FileCopyingPluginHandler(name: String) {
  private val suffix: String = "_${name}_plugin.jar"

  public fun setupPlugins(pluginsDir: Path, plugins: FileCollection) {
    setupPlugins(pluginsDir, plugins.files.map { it.toPath() })
  }

  public fun setupPlugins(pluginsDir: Path, plugins: List<Path>) {
    // Delete any jars left over from previous runs
    deleteOldPlugins(pluginsDir)

    // Create plugins dir if needed
    if (!pluginsDir.isDirectory()) {
      pluginsDir.createDirectories()
    }

    // Copy in plugins
    for (jar in plugins) {
      val name = jar.fileName.toString()
      jar.copyTo(pluginsDir.resolve(name.substring(0, name.length - 4 /*.jar*/) + suffix))
    }
  }

  public fun deleteOldPlugins(pluginsDir: Path) {
    if (pluginsDir.isDirectory()) {
      pluginsDir.listDirectoryEntries()
        .filter { it.isRegularFile() && it.name.endsWith(suffix) }
        .forEach { it.deleteIfExists() }
    }
  }
}
