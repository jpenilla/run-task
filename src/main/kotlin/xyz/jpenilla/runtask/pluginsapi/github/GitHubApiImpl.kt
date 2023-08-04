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
package xyz.jpenilla.runtask.pluginsapi.github

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import xyz.jpenilla.runtask.pluginsapi.GitHubApiDownload
import javax.inject.Inject

public abstract class GitHubApiImpl @Inject constructor(private val name: String, private val objects: ObjectFactory) : GitHubApi {

  private val jobs: MutableList<GitHubApiDownload> = mutableListOf()

  override fun getName(): String = name

  override fun add(owner: String, repo: String, tag: String, assetName: String) {
    val job = objects.newInstance(GitHubApiDownload::class)
    job.owner.set(owner)
    job.repo.set(repo)
    job.tag.set(tag)
    job.assetName.set(assetName)
    jobs += job
  }

  override fun addAllDownloads(downloads: Iterable<GitHubApiDownload>) {
    jobs.addAll(downloads)
  }

  override val downloads: Iterable<GitHubApiDownload>
    get() = jobs
}
