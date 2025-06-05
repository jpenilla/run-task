import xyz.jpenilla.runpaper.task.RunServer

plugins {
  id("xyz.jpenilla.run-paper")
  id("xyz.jpenilla.run-velocity")
  id("xyz.jpenilla.run-waterfall")
}

runPaper.folia.registerTask()

val paperPlugins = runPaper.downloadPluginsSpec {
  modrinth("carbon", "6dmNHzy8")
  github("jpenilla", "MiniMOTD", "v2.1.0", "minimotd-bukkit-2.1.0.jar")
  hangar("squaremap", "1.2.3")
  url("https://download.luckperms.net/1587/bukkit/loader/LuckPerms-Bukkit-5.5.2.jar")
  discord("1379024292548710400","1379024345845989440", project.property("bot_token") as String)
}

tasks {
  withType<RunServer> {
    minecraftVersion("1.20.4")
    runDirectory.set(layout.projectDirectory.dir("runServer"))
    downloadPlugins.from(paperPlugins)
  }
  runVelocity {
    version("3.3.0-SNAPSHOT")
    runDirectory.set(layout.projectDirectory.dir("runVelocity"))
    downloadPlugins {
      modrinth("minimotd", "z8DFFJMR")
      hangar("Carbon", "3.0.0-beta.26")
      url("https://download.luckperms.net/1530/velocity/LuckPerms-Velocity-5.4.117.jar")
    }
  }
}
