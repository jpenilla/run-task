# Run Paper

Run Paper is a Gradle plugin which adds a task to automatically download and run a Paper Minecraft server along with your plugin built by Gradle.

## Usage

In `settings.gradle.kts`, add the jpenilla repo for snapshot builds.

```kotlin
pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://repo.jpenilla.xyz/snapshots/")
  }
}
```

Apply the plugin in your project buildscript.

```kotlin
plugins {
  // Apply the plugin
  id("xyz.jpenilla.run-paper") version "1.0.0-SNAPSHOT"
}

tasks {
  runServer {
    // Configure the Minecraft version for our task.
    // This is the only required configuration besides applying the plugin.
    // Your plugin's jar (or shadowJar if present) will be used automatically.
    minecraftVersion("1.16.5")
  }
}
```

Now you can run a Paper server simply by invoking the `runServer` task!
