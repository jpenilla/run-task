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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
public data class BuildResponse(
  val id: Int,
  val time: String,
  val channel: String,
  val commits: List<Commit>,
  val downloads: Map<String, Download>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class Commit(
  val sha: String,
  val message: String,
  val time: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class Download(
  val name: String,
  val url: String,
  val size: Int,
  val checksums: Checksums,
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class Checksums(
  val sha256: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class ProjectsResponse(
  val projects: List<ProjectResponse>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class ProjectResponse(
  val project: Project,
  val versions: Map<String, List<String>>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class Project(
  val id: String,
  val name: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class VersionResponse(
  val version: Version,
  val builds: List<Int>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class Version(
  val id: String,
  val support: Support,
  val java: Java,
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class Support(
  val status: String,
  val end: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class Java(
  val version: JavaVersion,
  val flags: JavaFlags,
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class JavaVersion(
  val minimum: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class JavaFlags(
  val recommended: List<String>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class ErrorResponse(
  val error: String,
  val message: String,
  val ok: Boolean,
)
