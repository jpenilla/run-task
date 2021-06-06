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
package xyz.jpenilla.runpaper.util

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
    val elapsedMs = System.currentTimeMillis() - this.startTime
    val percentCompleted = bytesDownloaded.toDouble() / this.expectedSize.toDouble()

    val message = StringBuilder()

    message.append(this.prefix)
      .append(formatSize(bytesDownloaded))
      .append('/')
      .append(formatSize(this.expectedSize))
      .append(" <")
      .append(bar(25, percentCompleted))
      .append("> ")
      .append(PERCENT_DECIMAL_FORMAT.format(percentCompleted))

    // After 10 seconds start showing more detailed info (time elapsed and est. remaining)
    if (elapsedMs > 1000 * 10) {
      val allTimeForDownloading = elapsedMs * this.expectedSize / bytesDownloaded
      val roughEstimateRemainingMs = allTimeForDownloading - elapsedMs

      val elapsed = Duration.ofMillis(elapsedMs).prettyPrint()
      val remaining = Duration.ofMillis(roughEstimateRemainingMs).prettyPrint()

      val extra = ", $elapsed elapsed, est. $remaining remaining"
      message.append(extra)
    }

    return message.toString()
  }

  override fun onStart(expectedSize: Long) {
    this.startTime = System.currentTimeMillis()
    this.expectedSize = expectedSize.run {
      if (this == 0L) {
        // give some invalid progress instead of / by 0
        return@run 1L
      }
      this
    }
    this.onStart(this.state, this.createMessage(0L))
  }

  override fun updateProgress(bytesDownloaded: Long) {
    this.logger(this.state, this.createMessage(bytesDownloaded))
  }

  override fun close() {
    this.close(this.state)
  }
}
