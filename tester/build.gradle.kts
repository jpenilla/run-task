plugins {
  id("xyz.jpenilla.run-paper")
  id("xyz.jpenilla.run-velocity")
  id("xyz.jpenilla.run-waterfall")
}

tasks {
  runServer {
    minecraftVersion("1.20.1")
    runDirectory.set(layout.projectDirectory.dir("runServer"))
    downloadPlugins {
      // hangar("squaremap", "1.2.0")
      github("jpenilla", "squaremap", "v1.2.0", "squaremap-paper-mc1.20.1-1.2.0.jar")
    }
  }
  runVelocity {
    version("3.2.0-SNAPSHOT")
    runDirectory.set(layout.projectDirectory.dir("runVelocity"))
  }
}
