plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "2.2.0" apply false
    id("net.neoforged.moddev") version "2.0.147" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.147" apply false
}

stonecutter active "1.20.1-fabric" /* [SC] DO NOT EDIT */

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)
    properties { tags(version, loader) }
    constants.match(loader, "fabric", "forge", "neoforge")
}
