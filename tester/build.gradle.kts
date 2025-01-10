import xyz.jpenilla.runpaper.task.RunServer

plugins {
  java
  id("xyz.jpenilla.run-paper")
  id("xyz.jpenilla.run-velocity")
  id("xyz.jpenilla.run-waterfall")
}

java.toolchain {
  languageVersion = JavaLanguageVersion.of(21)
}

runPaper.folia.registerTask()

val paperPlugins = runPaper.downloadPluginsSpec {
  modrinth("carbon", "WPejrRaD")
  github("jpenilla", "MiniMOTD", "v2.1.5", "minimotd-bukkit-2.1.5.jar")
  hangar("squaremap", "1.3.4")
  url("https://download.luckperms.net/1569/bukkit/loader/LuckPerms-Bukkit-5.4.152.jar")
}

val toolchains = javaToolchains
tasks {
  register<RunServer>("run1_8") {
    version = "1.8.8"
    runDirectory = layout.projectDirectory.dir("run1_8")
    javaLauncher = toolchains.launcherFor { languageVersion = JavaLanguageVersion.of(11) }
    ignoreUnsupportedJvm()
  }
  register<RunServer>("run1_12") {
    version = "1.12.2"
    runDirectory = layout.projectDirectory.dir("run1_12")
    ignoreUnsupportedJvm()
  }
  withType<RunServer> {
    version.convention("1.21.4")
    runDirectory.convention(layout.projectDirectory.dir("runServer"))
  }
  runServer {
    downloadPlugins.from(paperPlugins)
  }
  runPaper.folia.task {
    downloadPlugins.from(paperPlugins)
  }
  runVelocity {
    version = "3.4.0-SNAPSHOT"
    runDirectory = layout.projectDirectory.dir("runVelocity")
    downloadPlugins {
      modrinth("minimotd", "nFRYRCht")
      hangar("Carbon", "3.0.0-beta.27")
      url("https://download.luckperms.net/1569/velocity/LuckPerms-Velocity-5.4.152.jar")
    }
  }
}
