import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`
  id("com.gradle.plugin-publish")
  id("net.kyori.indra")
  id("net.kyori.indra.license-header")
  id("net.kyori.indra.publishing.gradle-plugin")
  id("org.jlleitschuh.gradle.ktlint")
}

group = "xyz.jpenilla"
version = "2.0.0-SNAPSHOT"
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
    (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(8))
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
    dependsOn(licenseFormat, ktlintFormat)
  }
}

indra {
  apache2License()
  github("jpenilla", "run-paper")
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

license {
  header(file("LICENSE_HEADER"))
}

indraPluginPublishing {
  website("https://github.com/jpenilla/run-paper")
  plugin(
    "run-paper",
    "xyz.jpenilla.runpaper.RunPaperPlugin",
    "Run Paper",
    "Gradle plugin adding a task to run a Paper Minecraft server",
    listOf("minecraft", "server", "paper", "run")
  )
  plugin(
    "run-velocity",
    "xyz.jpenilla.runvelocity.RunVelocityPlugin",
    "Run Velocity",
    "Gradle plugin adding a task to run a Velocity proxy",
    listOf("minecraft", "proxy", "velocity", "run")
  )
}
