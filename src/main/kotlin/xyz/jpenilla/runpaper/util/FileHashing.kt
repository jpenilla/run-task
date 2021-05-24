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

import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

internal object FileHashing {
  private enum class HashingAlgorithm(val algorithmName: String) {
    SHA256("SHA-256")
  }

  fun sha256(input: File): String =
    this.toHexString(this.calculateHash(input, HashingAlgorithm.SHA256))

  private fun calculateHash(input: File, algorithm: HashingAlgorithm): ByteArray {
    val digest = try {
      MessageDigest.getInstance(algorithm.algorithmName)
    } catch (ex: NoSuchAlgorithmException) {
      throw RuntimeException(ex)
    }

    FileInputStream(input).use { fileInputStream ->
      val stream = DigestInputStream(fileInputStream, digest)
      stream.use { digestStream ->
        val buffer = ByteArray(1024)
        while (digestStream.read(buffer) != -1) {
          // reading
        }
      }
      return stream.messageDigest.digest()
    }
  }

  private fun toHexString(hash: ByteArray): String {
    val no = BigInteger(1, hash)
    var hashString = no.toString(16)
    while (hashString.length < 32) {
      hashString = "0$hashString"
    }
    return hashString
  }
}
