import xyz.jpenilla.runpaper.task.RunServer

plugins {
  id("xyz.jpenilla.run-paper")
  id("xyz.jpenilla.run-velocity")
  id("xyz.jpenilla.run-waterfall")
}

runPaper.folia.registerTask()

val paperPlugins = runPaper.downloadPluginsSpec {
  modrinth("carbon", "2.1.0-beta.21")
  github("jpenilla", "MiniMOTD", "v2.0.13", "minimotd-bukkit-2.0.13.jar")
  github("jpenilla", "squaremap", "v1.2.0", "squaremap-paper-mc1.20.1-1.2.0.jar")
  // hangar("squaremap", "1.2.0")
  url("https://download.luckperms.net/1515/bukkit/loader/LuckPerms-Bukkit-5.4.102.jar")
}

tasks {
  withType<RunServer> {
    minecraftVersion("1.20.1")
    runDirectory.set(layout.projectDirectory.dir("runServer"))
    downloadPlugins.from(paperPlugins)
  }
  runVelocity {
    version("3.2.0-SNAPSHOT")
    runDirectory.set(layout.projectDirectory.dir("runVelocity"))
    downloadPlugins {
      modrinth("carbon", "wn7XQHfd")
      url("https://download.luckperms.net/1515/velocity/LuckPerms-Velocity-5.4.102.jar")
    }
  }
}
