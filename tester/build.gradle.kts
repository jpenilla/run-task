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
  url("https://download.luckperms.net/1593/bukkit/loader/LuckPerms-Bukkit-5.5.8.jar")
}

tasks {
  withType<RunServer> {
    minecraftVersion("1.21.7")
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
  runWaterfall {
    version("1.21")
    runDirectory.set(layout.projectDirectory.dir("runWaterfall"))
  }
}
