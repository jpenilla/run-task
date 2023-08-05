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
package xyz.jpenilla.runtask.pluginsapi.hangar

import org.gradle.api.provider.Property
import xyz.jpenilla.runtask.pluginsapi.HangarApiDownload
import xyz.jpenilla.runtask.pluginsapi.PluginApi

public interface HangarApi : PluginApi<HangarApi, HangarApiDownload> {
  public val url: Property<String>

  public fun add(author: String, plugin: String, version: String)

  public operator fun invoke(author: String, plugin: String, version: String): Unit = add(author, plugin, version)
}
