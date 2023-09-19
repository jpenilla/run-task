import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`
  id("com.gradle.plugin-publish")
  id("net.kyori.indra")
  id("net.kyori.indra.publishing.gradle-plugin")
  id("net.kyori.indra.licenser.spotless")
}

group = "xyz.jpenilla"
version = "2.1.1-SNAPSHOT"
description = "Gradle plugins adding run tasks for Minecraft server and proxy software"

repositories {
  mavenCentral()
  gradlePluginPortal()
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  implementation(libs.bundles.jackson)
  compileOnly(libs.paperweightUserdev)
}

kotlin {
  explicitApi()
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions {
      apiVersion = "1.4"
      jvmTarget = "1.8"
      freeCompilerArgs = listOf("-opt-in=kotlin.io.path.ExperimentalPathApi")
    }
  }

  register("format") {
    group = "formatting"
    description = "Formats source code according to project style."
    dependsOn(spotlessApply)
  }
}

indra {
  apache2License()
  github("jpenilla", "run-task")
  publishSnapshotsTo("jmp", "https://repo.jpenilla.xyz/snapshots")
  configurePublications {
    pom {
      developers {
        developer {
          id.set("jmp")
          timezone.set("America/Phoenix")
        }
      }
    }
  }
}

indraSpotlessLicenser {
  licenseHeaderFile(rootProject.file("LICENSE_HEADER"))
}

spotless {
  val overrides = mapOf(
    "ktlint_standard_filename" to "disabled",
    "ktlint_standard_trailing-comma-on-call-site" to "disabled",
    "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
    "ktlint_standard_comment-wrapping" to "disabled", // allow block comments in between elements on the same line
  )
  kotlin {
    ktlint(libs.versions.ktlint.get()).editorConfigOverride(overrides)
  }
  kotlinGradle {
    ktlint(libs.versions.ktlint.get()).editorConfigOverride(overrides)
  }
}

fun tags(vararg extra: String): List<String> =
  listOf("minecraft", "papermc", "run", *extra)

indraPluginPublishing {
  website("https://github.com/jpenilla/run-task")
  plugin(
    "run-paper",
    "xyz.jpenilla.runpaper.RunPaperPlugin",
    "Run Paper",
    "Gradle plugin adding a task to run a Paper Minecraft server",
    tags("paper", "server")
  )
  plugin(
    "run-velocity",
    "xyz.jpenilla.runvelocity.RunVelocityPlugin",
    "Run Velocity",
    "Gradle plugin adding a task to run a Velocity proxy",
    tags("velocity", "proxy")
  )
  plugin(
    "run-waterfall",
    "xyz.jpenilla.runwaterfall.RunWaterfallPlugin",
    "Run Waterfall",
    "Gradle plugin adding a task to run a Waterfall proxy",
    tags("waterfall", "proxy")
  )
}
