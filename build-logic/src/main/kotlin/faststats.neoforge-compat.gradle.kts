plugins {
    id("net.neoforged.moddev")
    id("faststats.shadow-conventions")
    id("maven-publish")
}

val libs = the<VersionCatalogsExtension>().named("libs")

extra.set("publishArtifactId", "neoforge")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/neoforge")

tasks.processResources {
    from(project(":neoforge").file("src/main/resources"))
    inputs.property("version", provider { project.version })
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
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
