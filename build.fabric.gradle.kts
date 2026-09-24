plugins {
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin")
    `maven-publish`
}

val minecraftVersion = stonecutter.current.version
val requiredJava = when {
    stonecutter.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    stonecutter.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    stonecutter.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    stonecutter.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}
val fabricMinecraftRange: String = stonecutter.properties["mod.fabric_mc_range"]
val syrupLibraryVersion = "${property("deps.syrup_library")}+$minecraftVersion-fabric"

version = "${property("mod.version")}+$minecraftVersion-fabric"
group = property("mod.group") as String
val archiveName = property("mod.id") as String
base.archivesName = archiveName

repositories {
    maven("https://maven.syrupstudios.net/releases/")
    maven("https://jitpack.io")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    modImplementation("net.syrupstudios:syrup_library:$syrupLibraryVersion")
    compileOnly("org.projectlombok:lombok:${property("deps.lombok")}")
    annotationProcessor("org.projectlombok:lombok:${property("deps.lombok")}")
    implementation("com.github.Querz:NBT:6.1")
}

loom {
    fabricModJsonPath.set(rootProject.file("src/main/resources/fabric.mod.json"))
    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }
    runConfigs.configureEach {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run")
        jvmArguments.add("-Dmixin.debug.export=true")
    }
}

sourceSets.main { java.exclude("**/loaders/forge/**", "**/loaders/neoforge/**") }

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava
    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks.jar {
    from(rootProject.file("LICENSE.md")) { rename("(.*)", "\$1_$archiveName") }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(requiredJava.majorVersion.toInt())
}

tasks.processResources {
    val props = mapOf(
        "version" to project.property("mod.version"),
        "mc" to fabricMinecraftRange,
        "modId" to project.property("mod.id"),
        "modName" to project.property("mod.name"),
        "modDescription" to project.property("mod.description"),
        "authors" to project.property("mod.authors"),
        "license" to project.property("mod.license"),
        "sources" to project.property("mod.sources"),
        "syrupLibraryVersion" to syrupLibraryVersion.substringBefore('+'),
        "refmap" to "",
        "fl" to project.property("deps.fabric_loader"),
        "java" to requiredJava.majorVersion
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
    description = "Builds mod jars and copies results to `build/libs/{mod version}/`"
    inputs.property("version", project.property("mod.version"))
    from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = base.archivesName.get()
        }
    }
    repositories {
        maven {
            name = "syrupStudios"
            url = uri("https://maven.syrupstudios.net/releases/")
            credentials(PasswordCredentials::class)
        }
    }
}

val compatibleVersions = stonecutter.properties.rawOrNull("mod.mc_releases")?.asList()?.map { it.toString() }.orEmpty()
val outputFile = loomx.modJar.flatMap { it.archiveFile }
val changelogFile = rootProject.file("CHANGELOG.md")
val changelogText = providers.provider { if (changelogFile.isFile) changelogFile.readText() else "" }
val curseForgeToken = providers.gradleProperty("publish.curseforge_token").orElse(providers.environmentVariable("CURSEFORGE_TOKEN"))
val modrinthToken = providers.gradleProperty("publish.modrinth_token").orElse(providers.environmentVariable("MODRINTH_TOKEN"))

publishMods {
    file.set(outputFile)
    dryRun = providers.gradleProperty("publish.dry_run")
        .map { it.toBoolean() || !curseForgeToken.isPresent || !modrinthToken.isPresent }
        .orElse(!curseForgeToken.isPresent || !modrinthToken.isPresent)
    version = project.version.toString()
    displayName = "${property("mod.name")} ${property("mod.version")} for Minecraft ${minecraftVersion} (Fabric)"
    changelog = changelogText
    type = when (property("publish.release_type").toString().lowercase()) {
        "stable" -> STABLE
        "beta" -> BETA
        "alpha" -> ALPHA
        else -> throw GradleException("publish.release_type must be stable, beta, or alpha")
    }
    modLoaders.add("fabric")
    curseforge {
        projectId = property("publish.curseforge").toString()
        accessToken = curseForgeToken
        compatibleVersions.forEach(minecraftVersions::add)
        client = true
        server = true
        requires("syrup-library")
        requires("fabric-api")
    }
    modrinth {
        projectId = property("publish.modrinth").toString()
        accessToken = modrinthToken
        compatibleVersions.forEach(minecraftVersions::add)
        environment = CLIENT_AND_SERVER
        requires { id = "9tBNzmjo" }
        requires("fabric-api")
    }
}
