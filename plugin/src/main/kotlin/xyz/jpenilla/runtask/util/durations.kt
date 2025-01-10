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

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Map of accepted abbreviation [Char]s to [ChronoUnit].
 */
private val UNITS = mapOf(
  'd' to ChronoUnit.DAYS,
  'h' to ChronoUnit.HOURS,
  'm' to ChronoUnit.MINUTES,
  's' to ChronoUnit.SECONDS
)

/**
 * Parses a [Duration] from [input].
 *
 * Accepted format is a number followed by a unit abbreviation.
 * See [UNITS] for possible units.
 * Example input strings: `["1d", "12h", "1m", "30s"]`
 *
 * @param input formatted input string
 * @throws InvalidDurationException when [input] is improperly formatted
 */
@Throws(InvalidDurationException::class)
internal fun parseDuration(input: String): Duration {
  if (input.isBlank()) {
    throw InvalidDurationException.noInput(input)
  }
  if (input.length < 2) {
    throw InvalidDurationException.invalidInput(input)
  }
  val unitAbbreviation = input.last()

  val unit = UNITS[unitAbbreviation] ?: throw InvalidDurationException.invalidInput(input)

  val length = try {
    input.substring(0, input.length - 1).toLong()
  } catch (ex: NumberFormatException) {
    throw InvalidDurationException.invalidInput(input, ex)
  }

  return Duration.of(length, unit)
}

internal class InvalidDurationException private constructor(
  message: String,
  cause: Throwable? = null
) : IllegalArgumentException(message, cause) {
  internal companion object {
    private val infoMessage = """
      Accepted format is a number followed by a unit abbreviation.
      Possible units: $UNITS
      Example input strings: ["1d", "12h", "1m", "30s"]
    """.trimIndent()

    fun noInput(input: String): InvalidDurationException =
      InvalidDurationException("Cannot parse a Duration from a blank input string '$input'.\n$infoMessage")

    fun invalidInput(input: String, cause: Throwable? = null) =
      InvalidDurationException("Cannot parse a Duration from input '$input'.\n$infoMessage", cause)
  }
}

internal fun Duration.prettyPrint(): String =
  toString()
    .substring(2)
    .replace("(\\d[HMS])(?!$)".toRegex(), "$1 ")
    .lowercase(Locale.ENGLISH)
