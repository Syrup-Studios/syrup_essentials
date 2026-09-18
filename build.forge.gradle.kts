import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("net.neoforged.moddev.legacyforge") version "2.0.143"
    id("maven-publish")
}

val mcVersion = property("deps.minecraft") as String
val forgeVersion = property("deps.forge_version") as String
val targetJavaVersion = (property("deps.java_version") as String).toInt()
version = "${property("mod.version")}+$mcVersion-forge"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

repositories {
    maven("https://maven.syrupstudios.net/releases/")
    maven("https://jitpack.io")
}

legacyForge {
    setVersion("$mcVersion-$forgeVersion")
    runs { create("client") { client(); gameDirectory = project.file("run") } }
    mods.create(property("mod.id") as String) { sourceSet(sourceSets.main.get()) }
}

dependencies {
    modImplementation("net.syrupstudios:syrup_library:${property("syrup_library_version")}")
    compileOnly("org.projectlombok:lombok:${property("deps.lombok")}")
    annotationProcessor("org.projectlombok:lombok:${property("deps.lombok")}")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    implementation("com.github.Querz:NBT:6.1")
}

sourceSets.main { java.exclude("**/loaders/fabric/**", "**/loaders/neoforge/**") }
java {
    withSourcesJar()
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    sourceCompatibility = JavaVersion.toVersion(targetJavaVersion)
    targetCompatibility = JavaVersion.toVersion(targetJavaVersion)
}
tasks.jar {
    from(rootProject.file("LICENSE.md")) { rename { "${it}_${project.base.archivesName.get()}" } }
    manifest.attributes("MixinConfigs" to "syrup-essentials.mixins.json")
}
mixin {
    add(sourceSets.main.get(), "syrup-essentials.refmap.json")
    config("syrup-essentials.mixins.json")
}
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}
tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "mc" to mcVersion,
        "forge" to forgeVersion.substringBefore('.'),
        "modId" to project.property("mod.id"),
        "modName" to project.property("mod.name"),
        "modDescription" to project.property("mod.description"),
        "authors" to project.property("mod.authors"),
        "license" to project.property("mod.license"),
        "syrupLibraryVersion" to (project.property("syrup_library_version") as String).substringBefore('+'),
        "refmap" to "syrup-essentials.refmap.json"
    )
    inputs.properties(props)
    filesMatching("META-INF/mods.toml") { expand(props) }
    filesMatching("syrup-essentials.mixins.json") { expand(props) }
    from(rootProject.file("src/main/templates")) {
        include("pack.mcmeta")
        expand(props)
    }
    exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
}
tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.named("reobfJar"), tasks.named("sourcesJar"))
    into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    dependsOn("build")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.base.archivesName.get()
            from(components["java"])
        }
    }
}
