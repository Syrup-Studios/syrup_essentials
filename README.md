# Syrup Essentials

This repository currently builds the Fabric 1.20.1 target.

```sh
./gradlew build
./gradlew buildAndCollect
```

The active target is `1.20.1-fabric`, selected in `stonecutter.gradle.kts`. Target-specific settings are in `versions/1.20.1-fabric/gradle.properties`. Collected jars are written to `build/libs/0.3.0/`.

Gradle downloads the Java 17 toolchain automatically. You can run Gradle with a newer supported JDK.
