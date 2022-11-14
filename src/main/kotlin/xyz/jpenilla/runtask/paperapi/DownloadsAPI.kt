/*
 * Run Task Gradle Plugins
 * Copyright (c) 2021-2022 Jason Penilla
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
package xyz.jpenilla.runtask.paperapi

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URL

internal class DownloadsAPI(private val endpoint: String) {
  companion object {
    const val PAPER_ENDPOINT: String = "https://papermc.io/api/v2/"
    private val MAPPER: JsonMapper = JsonMapper.builder()
      .addModule(kotlinModule())
      .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
      .build()
  }

  /**
   * Make a query.
   *
   * @param R response type
   * @param query query
   * @return response
   */
  private inline fun <reified R> makeQuery(query: String): R {
    val response = URL(endpoint + query).readText(Charsets.UTF_8)
    return MAPPER.readValue(response)
  }

  fun projects(): ProjectsResponse {
    return makeQuery("projects")
  }

  fun project(projectName: String): ProjectResponse {
    return makeQuery("projects/$projectName")
  }

  fun versionGroup(projectName: String, versionGroup: String): VersionGroupResponse {
    return makeQuery("projects/$projectName/version_group/$versionGroup")
  }

  fun versionGroupBuilds(projectName: String, versionGroup: String): VersionGroupBuildsResponse {
    return makeQuery("projects/$projectName/version_group/$versionGroup/builds")
  }

  fun version(projectName: String, version: String): VersionResponse {
    return makeQuery("projects/$projectName/versions/$version")
  }

  fun build(projectName: String, version: String, build: Int): BuildResponse {
    return makeQuery("projects/$projectName/versions/$version/builds/$build")
  }

  fun downloadURL(projectName: String, version: String, build: Int, download: Download): String {
    return endpoint + "projects/$projectName/versions/$version/builds/$build/downloads/${download.name}"
  }
}
