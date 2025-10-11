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

import kotlinx.serialization.Serializable

@Serializable
internal data class BuildResponse(
  val id: Int,
  val time: String,
  val channel: String,
  val commits: List<Commit>,
  val downloads: Map<String, Download>,
)

@Serializable
internal data class Commit(
  val sha: String,
  val message: String,
  val time: String,
)

@Serializable
internal data class Download(
  val name: String,
  val url: String,
  val size: Int,
  val checksums: Checksums,
)

@Serializable
internal data class Checksums(
  val sha256: String,
)

@Serializable
internal data class ProjectsResponse(
  val projects: List<ProjectResponse>,
)

@Serializable
internal data class ProjectResponse(
  val project: Project,
  val versions: Map<String, List<String>>,
)

@Serializable
internal data class Project(
  val id: String,
  val name: String,
)

@Serializable
internal data class VersionResponse(
  val version: Version,
  val builds: List<Int>,
)

@Serializable
internal data class Version(
  val id: String,
  val support: Support,
  val java: Java,
)

@Serializable
internal data class Support(
  val status: String,
  val end: String? = null,
)

@Serializable
internal data class Java(
  val version: JavaVersion,
  val flags: JavaFlags,
)

@Serializable
internal data class JavaVersion(
  val minimum: Int,
)

@Serializable
internal data class JavaFlags(
  val recommended: List<String>,
)

@Serializable
internal data class ErrorResponse(
  val error: String,
  val message: String,
  val ok: Boolean,
)
