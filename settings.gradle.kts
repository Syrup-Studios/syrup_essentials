pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.fabricmc.net/")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.10"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "syrup-essentials"

stonecutter {
    create(rootProject) {
        version("1.20.1-fabric", "1.20.1").buildscript = "build.fabric.gradle"
        vcsVersion = "1.20.1-fabric"
    }
}
