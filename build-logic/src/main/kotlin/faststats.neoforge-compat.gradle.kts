plugins {
    id("net.neoforged.moddev")
    id("faststats.minecraft-version")
    id("faststats.onboarding-sources")
    id("faststats.shadow-conventions")
    id("maven-publish")
}

// version projects are configured by their parent before they have a catalog of their own
val libs = rootProject.the<VersionCatalogsExtension>().named("libs")
val minecraft = minecraftVersion

extra.set("publishArtifactId", "neoforge")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/neoforge")
extra.set("publishVersionSuffix", "mc$minecraft")

tasks.processResources {
    from(project(":neoforge").file("src/main/resources"))
    val version = provider { project.version.toString() }
    val range = "[${minecraft.name}]"
    inputs.property("version", version)
    inputs.property("minecraft", range)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to version.get(), "minecraft" to range)
    }
}

neoForge {
    version = libs.findVersion("neoforge-${minecraft.catalogName}").get().requiredVersion
}

configurations.configureEach {
    resolutionStrategy.force(libs.findLibrary("gson").get())
}

dependencies {
    compileOnly(libs.findLibrary("neoforged-bus").get())
    implementation(project(":core"))
    implementation(project(":config"))
    implementation(project(":onboarding"))
    implementation(project(":neoforge"))
}
