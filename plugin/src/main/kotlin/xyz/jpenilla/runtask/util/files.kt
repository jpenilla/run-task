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
package xyz.jpenilla.runtask.util

import org.gradle.api.Project
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemLocationProperty
import org.gradle.api.provider.Provider
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence
import kotlin.use

internal val FileSystemLocationProperty<*>.path: Path
  get() = get().path

internal fun FileSystemLocationProperty<*>.set(path: Path): Unit =
  set(path.toFile())

internal val Provider<out FileSystemLocation>.path: Path
  get() = get().path

internal val FileSystemLocation.path: Path
  get() = asFile.toPath()

internal val Project.sharedCaches: Path
  get() = gradle.gradleUserHomeDir.toPath().resolve(Constants.GRADLE_CACHES_DIRECTORY_NAME)

internal fun Path.walkMatching(glob: String): List<Path> = walkMatching {
  it.fileSystem.getPathMatcher("glob:$glob").matches(it)
}

internal fun Path.walkMatching(predicate: (Path) -> Boolean): List<Path> = Files.walk(this).use { stream ->
  stream.asSequence().filter { p -> predicate(p.relativeTo(this)) }.toList()
}
