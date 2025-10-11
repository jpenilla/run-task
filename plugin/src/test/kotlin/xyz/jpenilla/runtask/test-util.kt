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
package xyz.jpenilla.runtask

fun runWithRetries(times: Int = 3, block: () -> Unit) {
  val exceptions = mutableListOf<Exception>()
  var attempts = 0
  while (true) {
    try {
      block()
      return
    } catch (e: Exception) {
      exceptions += e
      if (++attempts >= times) {
        val exception = RuntimeException("Failed after $attempts attempts")
        exceptions.forEach { exception.addSuppressed(it) }
        throw exception
      }
      println("Attempt $attempts failed, retrying...")
      Thread.sleep(1000L * (1 shl (attempts - 1)))
    }
  }
}
