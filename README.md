# Run <Paper|Velocity|Waterfall>

[![build](https://img.shields.io/github/checks-status/jpenilla/run-task/master?label=build)](https://github.com/jpenilla/run-task/actions) [![license](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

This repository houses a suite of plugins which add tasks to automatically download and run Minecraft server/proxy
software along with your plugin built by Gradle. This provides a streamlined method of integration testing plugins.

<details>
<summary>Run Paper</summary>

[![latest release](https://img.shields.io/maven-metadata/v?label=gradle%20plugin%20portal&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fxyz%2Fjpenilla%2Frun-paper%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/xyz.jpenilla.run-paper)

### Basic Usage

In `build.gradle.kts`:

```kotlin
plugins {
  // Apply the plugin
  id("xyz.jpenilla.run-paper") version "2.0.0"
}

tasks {
  runServer {
    // Configure the Minecraft version for our task.
    // This is the only required configuration besides applying the plugin.
    // Your plugin's jar (or shadowJar if present) will be used automatically.
    minecraftVersion("1.19.2")
  }
}
```

You can now run a Paper server simply by invoking the `runServer` task!
</details>

<details>
<summary>Run Velocity</summary>

[![latest release](https://img.shields.io/maven-metadata/v?label=gradle%20plugin%20portal&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fxyz%2Fjpenilla%2Frun-velocity%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/xyz.jpenilla.run-velocity)

### Basic Usage

In `build.gradle.kts`:

```kotlin
plugins {
  // Apply the plugin
  id("xyz.jpenilla.run-velocity") version "2.0.0"
}

tasks {
  runVelocity {
    // Configure the Velocity version for our task.
    // This is the only required configuration besides applying the plugin.
    // Your plugin's jar (or shadowJar if present) will be used automatically.
    velocityVersion("3.1.2-SNAPSHOT")
  }
}
```

You can now run a Velocity proxy simply by invoking the `runVelocity` task!
</details>

<details>
<summary>Run Waterfall</summary>

[![latest release](https://img.shields.io/maven-metadata/v?label=gradle%20plugin%20portal&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fxyz%2Fjpenilla%2Frun-waterfall%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/xyz.jpenilla.run-waterfall)

### Basic Usage

In `build.gradle.kts`:

```kotlin
plugins {
  // Apply the plugin
  id("xyz.jpenilla.run-waterfall") version "2.0.0"
}

tasks {
  runWaterfall {
    // Configure the Waterfall version for our task.
    // This is the only required configuration besides applying the plugin.
    // Your plugin's jar (or shadowJar if present) will be used automatically.
    waterfallVersion("1.19")
  }
}
```

You can now run a Waterfall proxy simply by invoking the `runWaterfall` task!
</details>

Check out [the wiki](https://github.com/jpenilla/run-task/wiki) for more detailed usage information.
