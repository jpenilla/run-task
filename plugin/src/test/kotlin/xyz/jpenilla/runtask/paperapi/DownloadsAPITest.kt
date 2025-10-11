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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import xyz.jpenilla.runtask.runWithRetries

class DownloadsAPITest {
  private val api = DownloadsAPI(DownloadsAPI.PAPER_ENDPOINT)

  @Test
  fun testBuild() = runWithRetries {
    val buildNo = api.version(Projects.PAPER, "1.21.10").builds.last()
    val build = api.build(Projects.PAPER, "1.21.10", buildNo)
    assertNotNull(build)
  }

  @Test
  fun testLatestBuild() = runWithRetries {
    val build = api.latestBuild(Projects.PAPER, "1.21.10")
    assertNotNull(build)
  }
}
