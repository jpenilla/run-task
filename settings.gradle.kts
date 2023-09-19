pluginManagement {
  includeBuild("plugin")
}

plugins {
  id("ca.stellardrift.polyglot-version-catalogs") version "6.1.0"
}

rootProject.name = "run-task-parent"

include("tester")
