import xyz.jpenilla.runpaper.task.RunServer

plugins {
  java
  id("xyz.jpenilla.run-paper")
  id("xyz.jpenilla.run-velocity")
  id("xyz.jpenilla.run-waterfall")
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

runPaper.folia.registerTask()

val paperPlugins = runPaper.downloadPluginsSpec {
  modrinth("carbon", "2ptKcv94")
  github("jpenilla", "MiniMOTD", "v2.1.8", "minimotd-bukkit-2.1.8.jar")
  hangar("squaremap", "1.3.8")
  url("https://download.luckperms.net/1605/bukkit/loader/LuckPerms-Bukkit-5.5.16.jar")
}

tasks {
  withType<RunServer> {
    minecraftVersion("1.21.10")
    runDirectory.set(layout.projectDirectory.dir("runServer"))
    downloadPlugins.from(paperPlugins)
  }
  runVelocity {
    version("3.4.0-SNAPSHOT")
    runDirectory.set(layout.projectDirectory.dir("runVelocity"))
    downloadPlugins {
      modrinth("minimotd", "4ceIMQUi")
      hangar("Carbon", "3.0.0-beta.32")
      url("https://download.luckperms.net/1605/velocity/LuckPerms-Velocity-5.5.16.jar")
    }
  }
  runWaterfall {
    version("1.21")
    runDirectory.set(layout.projectDirectory.dir("runWaterfall"))
  }
}
