package xyz.jpenilla.runtask.util

import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence

internal fun maybeApplyPaperclip(
  javaLauncher: JavaLauncher,
  exec: ExecOperations,
  file: Path,
  workingDir: Path,
  resultRelativeTo: Path,
): List<String>? {
  val type = isPaperclip(file)
  if (type == PaperclipType.NONE) {
    return null
  }

  if (workingDir.exists()) {
    Files.walk(workingDir).use { stream ->
      stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
  }
  workingDir.createDirectories()
  applyPaperclip(javaLauncher, exec, file, workingDir)

  val classpath = mutableListOf<String>()
  val classpathPaths = mutableListOf<Path>()

  if (type == PaperclipType.MODERN) {
    val patchedJar = Files.walk(workingDir.resolve("versions")).use { stream ->
      stream.asSequence().filter { it.isRegularFile() && it.extension == "jar" }.single()
    }
    classpath += patchedJar.relativeTo(resultRelativeTo).toString()
    classpathPaths.add(patchedJar.normalize().absolute())

    classpath.add(workingDir.resolve("libraries").relativeTo(resultRelativeTo).toString() + "/**/*.jar")
    val libs = workingDir.walkMatching("libraries/**/*.jar")
    classpathPaths.addAll(libs)
  } else if (type == PaperclipType.LEGACY) {
    val patchedJar = workingDir.resolve("cache")
      .listDirectoryEntries()
      .single { it.name.startsWith("patched") && it.name.endsWith(".jar") }
    classpath += patchedJar.relativeTo(resultRelativeTo).toString()
    classpathPaths.add(patchedJar.normalize().absolute())
  }

  // Clean up leftover files in the working dir (i.e. vanilla jar)
  Files.walk(workingDir).use { stream ->
    stream.forEach {
      if (it.isRegularFile() && it.normalize().absolute() !in classpathPaths) {
        Files.delete(it)
        it.deleteEmptyParents()
      }
    }
  }

  return classpath
}

private enum class PaperclipType {
  NONE,
  LEGACY,
  MODERN
}

private fun isPaperclip(file: Path): PaperclipType {
  if (!file.isRegularFile()) {
    return PaperclipType.NONE
  }
  try {
    JarFile(file.toFile()).use {
      val main = it.manifest.mainAttributes.getValue("Main-Class")
      if (main == "com.destroystokyo.paperclip.Main") {
        return PaperclipType.LEGACY
      } else if (main == "io.papermc.paperclip.Paperclip") {
        return PaperclipType.LEGACY
      } else if (main == "io.papermc.paperclip.Main") {
        return PaperclipType.MODERN
      }
    }
  } catch (_: IOException) {
    return PaperclipType.NONE
  }
  return PaperclipType.NONE
}

private fun applyPaperclip(
  javaLauncher: JavaLauncher,
  exec: ExecOperations,
  paperclip: Path,
  workingDir: Path,
): ExecResult = exec.javaexec {
  executable = javaLauncher.executablePath.path.absolutePathString()
  classpath(paperclip)
  jvmArgs("-Dpaperclip.patchonly=true")
  workingDir(workingDir)
}.assertNormalExitValue()
