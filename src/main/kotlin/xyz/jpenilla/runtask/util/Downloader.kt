/*
 * Run Task Gradle Plugins
 * Copyright (c) 2021-2022 Jason Penilla
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

import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class Downloader(
  val remote: URL,
  val destination: Path
) {
  private var started = false

  @Volatile
  private var downloaded = 0L

  @Volatile
  private var expectedSize = 0L

  fun download(listener: ProgressListener): Result {
    if (started) {
      error("Cannot start download a second time.")
    }
    started = true

    val downloadFuture = CompletableFuture.runAsync {
      val connection = remote.openConnection()
      val expected = connection.contentLengthLong
      listener.onStart(expected)
      expectedSize = expected
      Channels.newChannel(connection.getInputStream()).use { remote ->
        val wrapped = ReadableByteChannelWrapper(remote) { bytesRead ->
          downloaded += bytesRead
        }
        FileOutputStream(destination.toFile()).use {
          it.channel.transferFrom(wrapped, 0, Long.MAX_VALUE)
        }
      }
    }

    val executor = Executors.newSingleThreadScheduledExecutor()
    val emitProgress = {
      if (expectedSize != 0L) {
        listener.updateProgress(downloaded)
      }
    }
    val task = executor.scheduleAtFixedRate(emitProgress, 0L, listener.updateRateMs, TimeUnit.MILLISECONDS)

    var failure: Throwable? = null
    try {
      downloadFuture.join()
    } catch (ex: CompletionException) {
      failure = ex.cause ?: ex
    } finally {
      task.cancel(true)
      executor.shutdownNow()
    }

    if (failure == null) {
      emitProgress()
      listener.close()
      return Result.Success(downloaded)
    }

    listener.close()
    return Result.Failure(failure)
  }

  sealed class Result {
    data class Failure(val throwable: Throwable) : Result()
    data class Success(val bytesDownloaded: Long) : Result()
  }

  interface ProgressListener : AutoCloseable {
    fun onStart(expectedSizeBytes: Long)
    fun updateProgress(bytesDownloaded: Long)
    val updateRateMs: Long
  }

  private class ReadableByteChannelWrapper(
    private val wrapped: ReadableByteChannel,
    private val readCallback: (Int) -> Unit
  ) : ReadableByteChannel by wrapped {
    @Throws(IOException::class)
    override fun read(byteBuffer: ByteBuffer): Int {
      val read = wrapped.read(byteBuffer)
      if (read > 0) {
        readCallback(read)
      }
      return read
    }
  }
}
