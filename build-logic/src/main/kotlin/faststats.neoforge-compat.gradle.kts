plugins {
    id("net.neoforged.moddev")
    id("faststats.shadow-conventions")
    id("maven-publish")
}

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
    resolutionStrategy.force("com.google.code.gson:gson:2.14.0")
}

dependencies {
    compileOnly("net.neoforged:bus:8.0.5")
    implementation(project(":core"))
    implementation(project(":config"))
    implementation(project(":onboarding"))
    implementation(project(":neoforge"))
}
