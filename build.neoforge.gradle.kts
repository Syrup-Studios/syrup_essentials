import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("net.neoforged.moddev") version "2.0.147"
    id("me.modmuss50.mod-publish-plugin") version "2.2.0"
    id("maven-publish")
}

val minecraftVersion = property("deps.minecraft") as String
val neoForgeVersion = property("deps.neoforge_version") as String
val targetJavaVersion = (property("deps.java_version") as String).toInt()
version = "${property("mod.version")}+$minecraftVersion-neoforge"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

repositories {
    maven("https://maven.syrupstudios.net/releases/")
    maven("https://jitpack.io")
}
neoForge {
    version = neoForgeVersion
    runs { create("client") { client(); gameDirectory = project.file("run") } }
    mods.create(property("mod.id") as String) { sourceSet(sourceSets.main.get()) }
}
dependencies {
    implementation("net.syrupstudios:syrup_library:${property("syrup_library_version")}")
    compileOnly("org.projectlombok:lombok:${property("deps.lombok")}")
    annotationProcessor("org.projectlombok:lombok:${property("deps.lombok")}")
    implementation("com.github.Querz:NBT:6.1")
}
sourceSets.main { java.exclude("**/loaders/fabric/**", "**/loaders/forge/**") }
java {
    withSourcesJar()
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    sourceCompatibility = JavaVersion.toVersion(targetJavaVersion)
    targetCompatibility = JavaVersion.toVersion(targetJavaVersion)
}
tasks.jar {
    from(rootProject.file("LICENSE.md")) { rename { "${it}_${project.base.archivesName.get()}" } }
}
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}
tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "mc" to minecraftVersion,
        "modId" to project.property("mod.id"),
        "modName" to project.property("mod.name"),
        "modDescription" to project.property("mod.description"),
        "authors" to project.property("mod.authors"),
        "license" to project.property("mod.license"),
        "syrupLibraryVersion" to (project.property("syrup_library_version") as String).substringBefore('+'),
        "refmap" to ""
    )
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
    filesMatching("syrup-essentials.mixins.json") {
        expand(props)
        filter { line: String -> line.replace("\"refmap\": \"\",", "") }
    }
    exclude("fabric.mod.json", "META-INF/mods.toml")
}
tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.named("jar"), tasks.named("sourcesJar"))
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

apply(from = rootProject.file("gradle/platform-publishing.gradle"))
