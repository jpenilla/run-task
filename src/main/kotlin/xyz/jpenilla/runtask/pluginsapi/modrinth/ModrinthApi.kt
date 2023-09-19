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
package xyz.jpenilla.runtask.pluginsapi.modrinth

import org.gradle.api.provider.Property
import xyz.jpenilla.runtask.pluginsapi.ModrinthApiDownload
import xyz.jpenilla.runtask.pluginsapi.PluginApi

public interface ModrinthApi : PluginApi<ModrinthApiDownload> {
  public val url: Property<String>

  public fun add(id: String, version: String)

  public operator fun invoke(id: String, version: String): Unit = add(id, version)
}
