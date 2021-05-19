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
package xyz.jpenilla.runpaper.extension

import de.undercouch.gradle.tasks.download.DownloadAction
import de.undercouch.gradle.tasks.download.DownloadExtension
import de.undercouch.gradle.tasks.download.VerifyAction
import de.undercouch.gradle.tasks.download.VerifyExtension
import org.gradle.api.Task
import org.gradle.kotlin.dsl.delegateClosureOf
import org.gradle.kotlin.dsl.getByType

/**
 * Downloads a file based on the supplied configuration block.
 *
 * @param configurer configuration block
 */
public fun Task.download(configurer: DownloadAction.() -> Unit): DownloadExtension =
  this.project.extensions.getByType<DownloadExtension>().configure(delegateClosureOf(configurer))

/**
 * Verifies a given file by comparing it's hash to a known value.
 * Will throw an exception failing the current task if verification fails.
 *
 * @param configurer configuration block
 */
public fun Task.verify(configurer: VerifyAction.() -> Unit): VerifyExtension =
  this.project.extensions.getByType<VerifyExtension>().configure(delegateClosureOf(configurer))
