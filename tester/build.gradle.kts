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
  modrinth("carbon", "6dmNHzy8")
  github("jpenilla", "MiniMOTD", "v2.1.6", "minimotd-bukkit-2.1.6.jar")
  hangar("squaremap", "1.3.6")
  url("https://download.luckperms.net/1593/bukkit/loader/LuckPerms-Bukkit-5.5.8.jar")
  discord("1379024292548710400","1379024345845989440", project.property("bot_token") as String)
}

tasks {
  withType<RunServer> {
    minecraftVersion("1.21.7")
    runDirectory.set(layout.projectDirectory.dir("runServer"))
    downloadPlugins.from(paperPlugins)
  }
  runVelocity {
    version("3.4.0-SNAPSHOT")
    runDirectory.set(layout.projectDirectory.dir("runVelocity"))
    downloadPlugins {
      modrinth("minimotd", "z8DFFJMR")
      hangar("Carbon", "3.0.0-beta.26")
      url("https://download.luckperms.net/1594/velocity/LuckPerms-Velocity-5.5.9.jar")
    }
  }
  runWaterfall {
    version("1.21")
    runDirectory.set(layout.projectDirectory.dir("runWaterfall"))
  }
}
