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
package xyz.jpenilla.runpaper

internal fun String.minecraftVersionIsSameOrNewerThan(vararg other: Int): Boolean {
  val minecraft = split(".").map {
    try {
      it.toInt()
    } catch (ex: NumberFormatException) {
      return true
    }
  }

  for ((current, target) in minecraft zip other.toList()) {
    if (current < target) return false
    if (current > target) return true
    // If equal, check next subversion
  }

  // version is same
  return true
}
