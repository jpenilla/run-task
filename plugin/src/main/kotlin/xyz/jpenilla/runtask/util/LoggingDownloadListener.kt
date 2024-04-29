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

import java.text.DecimalFormat
import java.time.Duration
import kotlin.math.log10
import kotlin.math.pow

internal class LoggingDownloadListener<S>(
  private val state: S,
  private val onStart: (S, String) -> Unit = { _, _ -> },
  private val logger: (S, String) -> Unit,
  private val close: (S) -> Unit = { _ -> },
  private val prefix: String = "",
  override val updateRateMs: Long
) : Downloader.ProgressListener {
  private var startTime: Long = 0L
  private var expectedSize: Long = 0L

  private companion object {
    val PERCENT_DECIMAL_FORMAT = DecimalFormat("##0.##%")
    val SIZE_DECIMAL_FORMAT = DecimalFormat("#,##0.##")
    val UNITS = arrayOf("B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB")

    fun formatSize(size: Long): String {
      if (size <= 0) return "0 B"
      val digitGroups = (log10(size.toDouble()) / log10(1024.00)).toInt()
      return SIZE_DECIMAL_FORMAT.format(size / 1024.00.pow(digitGroups.toDouble())) + " " + UNITS[digitGroups]
    }

    fun bar(length: Int, percent: Double): String {
      val builder = StringBuilder()
      for (i in 1..length) {
        builder.append(if (i <= length * percent) '=' else '-')
      }
      return builder.toString()
    }
  }

  private fun createMessage(bytesDownloaded: Long): String {
    val elapsedMs = System.currentTimeMillis() - startTime

    val message = StringBuilder()

    if (expectedSize <= 0) {
      message.append(prefix)
        .append(formatSize(bytesDownloaded))
        .append('/')
        .append("?")
    } else {
      val percentCompleted = bytesDownloaded.toDouble() / expectedSize.toDouble()
      message.append(prefix)
        .append(formatSize(bytesDownloaded))
        .append('/')
        .append(formatSize(expectedSize))
        .append(" <")
        .append(bar(25, percentCompleted))
        .append("> ")
        .append(PERCENT_DECIMAL_FORMAT.format(percentCompleted))
    }

    // After 10 seconds start showing more detailed info (time elapsed and est. remaining)
    if (elapsedMs > 1000 * 10) {
      val elapsed = Duration.ofMillis(elapsedMs).prettyPrint()

      val remaining = if (expectedSize <= 0) {
        "unknown time"
      } else {
        val allTimeForDownloading = elapsedMs * expectedSize / bytesDownloaded
        val roughEstimateRemainingMs = allTimeForDownloading - elapsedMs
        "est. " + Duration.ofMillis(roughEstimateRemainingMs).prettyPrint()
      }

      val extra = ", $elapsed elapsed, $remaining remaining"
      message.append(extra)
    }

    return message.toString()
  }

  override fun onStart(expectedSizeBytes: Long) {
    startTime = System.currentTimeMillis()
    expectedSize = expectedSizeBytes
    onStart(state, createMessage(0L))
  }

  override fun updateProgress(bytesDownloaded: Long) {
    logger(state, createMessage(bytesDownloaded))
  }

  override fun close() {
    close(state)
  }
}
