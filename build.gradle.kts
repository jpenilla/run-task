plugins {
  `kotlin-dsl`
  id("com.gradle.plugin-publish")
  id("net.kyori.indra")
  id("net.kyori.indra.license-header")
  id("net.kyori.indra.publishing.gradle-plugin")
}

group = "xyz.jpenilla"
version = "1.0.0"
description = "Gradle plugin adding a task to run a Paper Minecraft server"

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(libs.bundles.jackson)
  implementation(libs.shadow)
}

kotlin {
  explicitApi()
}

tasks {
  compileKotlin {
    kotlinOptions.apiVersion = "1.4"
    kotlinOptions.jvmTarget = "1.8"
  }
}

indra {
  javaVersions {
    target(8)
  }
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
