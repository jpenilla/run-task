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
package xyz.jpenilla.runtask.pluginsapi.url

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import xyz.jpenilla.runtask.pluginsapi.UrlDownload
import javax.inject.Inject

public abstract class UrlPluginProviderImpl @Inject constructor(private val name: String, private val objects: ObjectFactory) : UrlPluginProvider {

  private val jobs: MutableList<UrlDownload> = mutableListOf()

  override fun getName(): String = name

  override fun add(url: String) {
    val job = objects.newInstance(UrlDownload::class)
    job.url.set(url)
    jobs += job
  }

  override fun copyConfiguration(api: UrlPluginProvider) {
    jobs.addAll(api.downloads)
  }

  override val downloads: Iterable<UrlDownload>
    get() = jobs
}
