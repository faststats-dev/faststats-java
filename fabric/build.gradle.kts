extra.set("moduleName", "dev.faststats.fabric")
extra.set("publishArtifactId", "fabric")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/fabric")

plugins {
    id("net.fabricmc.fabric-loom")
    id("maven-publish")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(17)
}

configurations.compileClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}

tasks.processResources {
    val version = provider { project.version.toString() }
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand("version" to version.get(), "minecraft" to "*")
    }
}

dependencies {
    compileOnlyApi(project(":core"))
    compileOnly(project(":config"))
    compileOnly(project(":onboarding"))
    minecraft(libs.minecraft.v26x1x2)
    compileOnly(libs.fabric.api.v26x1x2)
    compileOnly(libs.fabric.loader)
}

subprojects {
    if (parent?.path == ":fabric:versions") pluginManager.apply("faststats.fabric-compat")
}
