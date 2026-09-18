import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.17.14" apply false
    id("net.fabricmc.fabric-loom") version "1.17.14" apply false
    id("maven-publish")
}

val remappedMinecraft = stonecutter.eval(stonecutter.current.version, "<26")
val minecraftVersion = property("deps.minecraft") as String
val targetJavaVersion = (property("deps.java_version") as String).toInt()
val requiredJava = JavaVersion.toVersion(targetJavaVersion)
apply(plugin = if (remappedMinecraft) "net.fabricmc.fabric-loom-remap" else "net.fabricmc.fabric-loom")

version = "${property("mod.version")}+$minecraftVersion-fabric"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

repositories {
    maven("https://maven.syrupstudios.net/releases/")
    maven("https://jitpack.io")
}

val loomExtension = extensions.getByType<LoomGradleExtensionAPI>()
dependencies {
    add("minecraft", "com.mojang:minecraft:$minecraftVersion")
    if (remappedMinecraft) add("mappings", loomExtension.officialMojangMappings())
    val config = if (remappedMinecraft) "modImplementation" else "implementation"
    add(config, "net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    add(config, "net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    add(config, "net.syrupstudios:syrup_library:${property("syrup_library_version")}")
    compileOnly("org.projectlombok:lombok:${property("deps.lombok")}")
    annotationProcessor("org.projectlombok:lombok:${property("deps.lombok")}")
    implementation("com.github.Querz:NBT:6.1")
}

loomExtension.apply {
    enableTransitiveAccessWideners.set(false)
    fabricModJsonPath.set(rootProject.file("src/main/resources/fabric.mod.json"))
    if (remappedMinecraft) decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }
    runConfigs.configureEach { runDir = "run" }
}

sourceSets.main { java.exclude("**/loaders/forge/**", "**/loaders/neoforge/**") }

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava
    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
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
        "modName" to project.property("mod.name"),
        "modDescription" to project.property("mod.description"),
        "authors" to project.property("mod.authors"),
        "license" to project.property("mod.license"),
        "refmap" to "",
        "fl" to project.property("deps.fabric_loader"),
        "java" to targetJavaVersion
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
    filesMatching("syrup-essentials.mixins.json") {
        expand(props)
        filter { line: String -> line.replace("\"refmap\": \"\",", "") }
    }
    exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml")
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    val productionJar = if (remappedMinecraft) "remapJar" else "jar"
    val sourceJar = if (remappedMinecraft) "remapSourcesJar" else "sourcesJar"
    from(tasks.named(productionJar), tasks.named(sourceJar))
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
