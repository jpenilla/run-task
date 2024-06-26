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
package xyz.jpenilla.runtask.pluginsapi.modrinth

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import xyz.jpenilla.runtask.pluginsapi.ModrinthApiDownload
import javax.inject.Inject

public abstract class ModrinthApiImpl @Inject constructor(private val name: String, private val objects: ObjectFactory) : ModrinthApi {

  private val jobs: MutableList<ModrinthApiDownload> = mutableListOf()

  override fun getName(): String = name

  override val url: Property<String> = objects.property()

  override fun add(id: String, version: String) {
    val job = objects.newInstance(ModrinthApiDownload::class)
    job.url.set(url.map { it.trimEnd('/') })
    job.id.set(id)
    job.version.set(version)
    jobs.add(job)
  }

  override fun copyConfiguration(api: ModrinthApi) {
    url.set(api.url)
    jobs.addAll(api.downloads)
  }

  override val downloads: Iterable<ModrinthApiDownload>
    get() = jobs
}
