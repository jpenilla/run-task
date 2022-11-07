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
version = "1.1.0-SNAPSHOT"
description = "Gradle plugin adding a task to run a Paper Minecraft server"

repositories {
  mavenCentral()
  gradlePluginPortal()
  maven("https://papermc.io/repo/repository/maven-public/")
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
          timezone.set("America/Los Angeles")
        }
      }
    }
  }
}

license {
  header(file("LICENSE_HEADER"))
}

indraPluginPublishing {
  plugin(
    "run-paper",
    "xyz.jpenilla.runpaper.RunPaper",
    "Run Paper",
    project.description,
    listOf("minecraft", "paper", "run")
  )
  website("https://github.com/jpenilla/run-paper")
}
