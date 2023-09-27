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

import xyz.jpenilla.runtask.pluginsapi.JenkinsDownload
import xyz.jpenilla.runtask.pluginsapi.PluginApi

/**
 * [PluginApi] implementation for downloading plugins from Jenkins CI.
 */
public interface JenkinsPluginProvider : PluginApi<JenkinsPluginProvider, JenkinsDownload> {

  /**
   * Add a plugin download.
   *
   * @param baseUrl The root url to the jenkins instance
   * @param job The id of the target job to download the plugin from
   * @param artifactRegex In case multiple artifacts are provided, a regex to pick the correct artifact
   * @param build A specific build for the [job] or the lastSuccessfulBuild if none provided
   */
  public fun add(baseUrl: String, job: String, artifactRegex: Regex? = null, build: String? = null)
}
