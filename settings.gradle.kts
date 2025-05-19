pluginManagement {
  includeBuild("plugin")
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "run-task-parent"

include("tester")
