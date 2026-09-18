plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.20.1-fabric"

stonecutter {
    parameters {
        val loader = node.metadata.project.substringAfterLast('-')
        constants.match(loader, "fabric", "forge", "neoforge")
    }
}
