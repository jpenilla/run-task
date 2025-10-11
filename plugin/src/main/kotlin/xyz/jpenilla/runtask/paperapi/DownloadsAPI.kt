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
package xyz.jpenilla.runtask.paperapi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import xyz.jpenilla.runtask.util.Constants
import java.net.HttpURLConnection
import java.net.URL

internal class DownloadsAPI(private val endpoint: String) {
  companion object {
    const val PAPER_ENDPOINT: String = "https://fill.papermc.io/v3/"

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
      ignoreUnknownKeys = true
      namingStrategy = JsonNamingStrategy.SnakeCase
    }
  }

  /**
   * Make a query.
   *
   * @param R response type
   * @param query query
   * @return response
   */
  private inline fun <reified R> makeQuery(query: String): R {
    val url = URL(endpoint + query)
    val connection = url.openConnection() as HttpURLConnection
    var response: String? = null
    try {
      connection.setRequestProperty("User-Agent", Constants.USER_AGENT)
      connection.setRequestProperty("Accept", "application/json")
      connection.connect()

      response = connection.inputStream.bufferedReader().use { it.readText() }
      return json.decodeFromString(response)
    } catch (ex: SerializationException) {
      throw IllegalStateException("Failed to parse response from $url\n${response ?: "null response"}", ex)
    } finally {
      connection.disconnect()
    }
  }

  fun projects(): ProjectsResponse {
    return makeQuery("projects")
  }

  fun project(projectName: String): ProjectResponse {
    return makeQuery("projects/$projectName")
  }

  fun versions(projectName: String): List<VersionResponse> {
    return makeQuery("projects/$projectName/versions")
  }

  fun version(projectName: String, version: String): VersionResponse {
    return makeQuery("projects/$projectName/versions/$version")
  }

  fun builds(projectName: String, version: String): List<BuildResponse> {
    return makeQuery("projects/$projectName/versions/$version/builds")
  }

  fun latestBuild(projectName: String, version: String): BuildResponse {
    return makeQuery("projects/$projectName/versions/$version/builds/latest")
  }

  fun build(projectName: String, version: String, build: Int): BuildResponse {
    return makeQuery("projects/$projectName/versions/$version/builds/$build")
  }

  fun downloadURL(download: Download): String {
    return download.url
  }
}
