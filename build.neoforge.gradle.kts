plugins {
    id("net.neoforged.moddev")
    id("neoforge-mutex")
    id("me.modmuss50.mod-publish-plugin")
    `maven-publish`
}

val minecraftVersion = stonecutter.current.version
val neoForgeVersion = property("deps.neoforge_version") as String
val syrupLibraryVersion = "${property("deps.syrup_library")}+$minecraftVersion-neoforge"
val requiredJava = when {
    stonecutter.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    stonecutter.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    stonecutter.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    stonecutter.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}
val neoForgeMinecraftRange: String = stonecutter.properties["mod.neoforge_mc_range"]

version = "${property("mod.version")}+$minecraftVersion-neoforge"
group = property("mod.group") as String
val archiveName = property("mod.id") as String
base.archivesName = archiveName

repositories {
    maven("https://maven.syrupstudios.net/releases/")
    maven("https://jitpack.io")
}
neoForge {
    version = neoForgeVersion
    runs {
        create("client") { client(); gameDirectory = rootProject.file("run") }
        create("server") { server(); gameDirectory = rootProject.file("run") }
    }
    mods.create(property("mod.id") as String) { sourceSet(sourceSets.main.get()) }
}
dependencies {
    implementation("net.syrupstudios:syrup_library:$syrupLibraryVersion")
    compileOnly("org.projectlombok:lombok:${property("deps.lombok")}")
    annotationProcessor("org.projectlombok:lombok:${property("deps.lombok")}")
    implementation("com.github.Querz:NBT:6.1")
}
sourceSets.main { java.exclude("**/loaders/fabric/**", "**/loaders/forge/**") }
java {
    withSourcesJar()
    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava
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
        "mc" to neoForgeMinecraftRange,
        "modId" to project.property("mod.id"),
        "modName" to project.property("mod.name"),
        "modDescription" to project.property("mod.description"),
        "authors" to project.property("mod.authors"),
        "license" to project.property("mod.license"),
        "syrupLibraryVersion" to syrupLibraryVersion.substringBefore('+'),
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
    description = "Builds mod jars and copies results to `build/libs/{mod version}/`"
    inputs.property("version", project.property("mod.version"))
    from(
        tasks.named<Jar>("jar").flatMap { it.archiveFile },
        tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile }
    )
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
val outputFile = tasks.named<Jar>("jar").flatMap { it.archiveFile }
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
    displayName = "${property("mod.name")} ${property("mod.version")} for Minecraft ${minecraftVersion} (NeoForge)"
    changelog = changelogText
    type = when (property("publish.release_type").toString().lowercase()) {
        "stable" -> STABLE
        "beta" -> BETA
        "alpha" -> ALPHA
        else -> throw GradleException("publish.release_type must be stable, beta, or alpha")
    }
    modLoaders.add("neoforge")
    curseforge {
        projectId = property("publish.curseforge").toString()
        accessToken = curseForgeToken
        compatibleVersions.forEach(minecraftVersions::add)
        client = true
        server = true
        requires("syrup-library")
    }
    modrinth {
        projectId = property("publish.modrinth").toString()
        accessToken = modrinthToken
        compatibleVersions.forEach(minecraftVersions::add)
        environment = CLIENT_AND_SERVER
        requires { id = "9tBNzmjo" }
    }
}
