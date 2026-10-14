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
    val version = provider { project.version.toString() }
    inputs.property("version", version)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to version.get(), "minecraft" to "[1.20.6,)")
    }
}

neoForge {
    version = libs.versions.neoforge.v1x21x8.get() // lowest bound, 1.20.6
}

configurations.configureEach {
    resolutionStrategy.force(libs.gson)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":config"))
    compileOnlyApi(project(":onboarding"))
    compileOnly(libs.neoforged.bus)
}

subprojects {
    if (parent?.path == ":neoforge:versions") pluginManager.apply("faststats.neoforge-compat")
}
