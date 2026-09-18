# Syrup Essentials

This repository builds ten Fabric, Forge, and NeoForge targets.

- Minecraft 1.20.1: Fabric and Forge
- Minecraft 1.21.1: Fabric and NeoForge
- Minecraft 1.21.11: Fabric and NeoForge
- Minecraft 26.2: Fabric and NeoForge
- Minecraft 26.3: Fabric and NeoForge

```sh
./gradlew build
./gradlew buildAndCollect
```

The active target is `1.20.1-fabric`, selected in `stonecutter.gradle.kts`. Target-specific settings are in `versions/*/gradle.properties`. Collected jars are written to `build/libs/0.4.0/`.

Gradle downloads the Java 17, 21, or 25 toolchain for the selected target.
