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
package xyz.jpenilla.runtask.pluginsapi.jenkins

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import xyz.jpenilla.runtask.pluginsapi.JenkinsDownload
import javax.inject.Inject

public abstract class JenkinsPluginProviderImpl @Inject constructor(private val name: String, private val objects: ObjectFactory) : JenkinsPluginProvider {

  private val jobs: MutableList<JenkinsDownload> = mutableListOf()

  override fun getName(): String = name

  override fun add(baseUrl: String, job: String, artifactRegex: Regex?, build: String?) {
    val download = objects.newInstance(JenkinsDownload::class)
    download.baseUrl.set(baseUrl)
    download.job.set(job)
    if (artifactRegex != null) {
      download.artifactRegex.set(artifactRegex)
    }
    if (build != null) {
      download.build.set(build)
    }
    jobs += download
  }

  override fun copyConfiguration(api: JenkinsPluginProvider) {
    jobs.addAll(api.downloads)
  }

  override val downloads: Iterable<JenkinsDownload>
    get() = jobs
}
