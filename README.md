# Run Paper

[![build](https://img.shields.io/github/checks-status/jpenilla/run-paper/master?label=build)](https://github.com/jpenilla/run-paper/actions) [![license](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE) [![latest release](https://img.shields.io/github/v/release/jpenilla/run-paper)](https://plugins.gradle.org/plugin/xyz.jpenilla.run-paper)

Run Paper is a Gradle plugin which adds a task to automatically download and run a Paper Minecraft server along with your plugin built by Gradle.

### Usage

Apply the plugin in your project buildscript.

```kotlin
plugins {
  // Apply the plugin
  id("xyz.jpenilla.run-paper") version "1.0.1"
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
