/*
 * Run Paper Gradle Plugin
 * Copyright (c) 2021 Jason Penilla
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
package xyz.jpenilla.runpaper.paperapi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class BuildResponse(
  val projectId: String,
  val projectName: String,
  val version: String,
  val build: Int,
  val time: String,
  val changes: List<Change>,
  val downloads: Map<String, Download>,
  val channel: String,
  val promoted: Boolean,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Change(
  val commit: String,
  val summary: String,
  val message: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Download(
  val name: String,
  val sha256: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ProjectsResponse(
  val projects: List<String>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ProjectResponse(
  val projectId: String,
  val projectName: String,
  val versionGroups: List<String>,
  val versions: List<String>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class VersionGroupBuild(
  val build: Int,
  val time: String,
  val changes: List<Change>,
  val version: String,
  val downloads: Map<String, Download>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class VersionGroupBuildsResponse(
  val projectId: String,
  val projectName: String,
  val versionGroup: String,
  val versions: List<String>,
  val builds: List<VersionGroupBuild>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class VersionGroupResponse(
  val projectId: String,
  val projectName: String,
  val versionGroup: String,
  val versions: List<String>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class VersionResponse(
  val projectId: String,
  val projectName: String,
  val version: String,
  val builds: List<Int>,
)
