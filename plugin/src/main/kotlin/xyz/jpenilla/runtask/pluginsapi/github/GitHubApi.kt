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

import xyz.jpenilla.runtask.pluginsapi.GitHubApiDownload
import xyz.jpenilla.runtask.pluginsapi.PluginApi

/**
 * [PluginApi] implementation for GitHub Releases.
 */
public interface GitHubApi : PluginApi<GitHubApi, GitHubApiDownload> {
  /**
   * Add a release artifact plugin download.
   *
   * @param owner repo owner
   * @param repo repo name
   * @param tag release tag
   * @param assetName asset file name
   */
  public fun add(owner: String, repo: String, tag: String, assetName: String)
}
