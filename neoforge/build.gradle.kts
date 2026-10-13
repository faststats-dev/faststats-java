extra.set("moduleName", "dev.faststats.neoforge")
extra.set("publishArtifactId", "neoforge")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/neoforge")

plugins {
    id("net.neoforged.moddev")
    id("faststats.shadow-conventions")
    id("maven-publish")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(21)
}

configurations.compileClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}

configurations.runtimeClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}

tasks.processResources {
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
}

neoForge {
    version = "21.8.53" // lowest bound, 1.20.6
}

configurations.configureEach {
    resolutionStrategy.force("com.google.code.gson:gson:2.14.0")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":config"))
    compileOnlyApi(project(":onboarding"))
    compileOnly("net.neoforged:bus:8.0.5")
}
