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

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

private enum class HashingAlgorithm(val algorithmName: String) {
  MD5("MD5"),
  SHA256("SHA-256");

  fun digest(): MessageDigest = try {
    MessageDigest.getInstance(this.algorithmName)
  } catch (ex: NoSuchAlgorithmException) {
    throw RuntimeException("Could not get MessageDigest instance for '${this.algorithmName}' algorithm.", ex)
  }
}

internal fun Path.sha256(): String =
  toHexString(this.calculateHash(HashingAlgorithm.SHA256))

private fun Path.calculateHash(algorithm: HashingAlgorithm): ByteArray =
  Files.newInputStream(this).calculateHash(algorithm)

private fun InputStream.calculateHash(algorithm: HashingAlgorithm): ByteArray = use { inputStream ->
  val stream = DigestInputStream(inputStream, algorithm.digest())
  stream.use { digestStream ->
    val buffer = ByteArray(1024)
    while (digestStream.read(buffer) != -1) {
      // reading
    }
  }
  return stream.messageDigest.digest()
}

private fun toHexString(hash: ByteArray): String {
  val hexString = StringBuilder(2 * hash.size)
  for (byte in hash) {
    val hex = Integer.toHexString(0xff and byte.toInt())
    if (hex.length == 1) {
      hexString.append('0')
    }
    hexString.append(hex)
  }
  return hexString.toString()
}
