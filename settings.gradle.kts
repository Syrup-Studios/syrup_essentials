pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.10"
    id("dev.kikugie.loom-back-compat") version "0.3"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = providers.gradleProperty("mod.id").get()

stonecutter {
    create(rootProject) {
        fun target(version: String, loader: String, buildscript: String = loader) {
            version("$version-$loader", version).buildscript = "build.$buildscript.gradle.kts"
        }

        target("1.20.1", "fabric")
        target("1.20.1", "forge")
        target("1.21.1", "fabric")
        target("1.21.1", "neoforge")
        target("1.21.11", "fabric")
        target("1.21.11", "neoforge")
        target("26.2", "fabric")
        target("26.2", "neoforge")
        target("26.3", "fabric")
        target("26.3", "neoforge")

        vcsVersion = "1.20.1-fabric"
    }
}
