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
      hangar("squaremap", "1.2.0")
    }
  }
  runVelocity {
    version("3.2.0-SNAPSHOT")
    runDirectory.set(layout.projectDirectory.dir("runVelocity"))
  }
}
