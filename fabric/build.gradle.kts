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
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

dependencies {
    compileOnlyApi(project(":core"))
    compileOnly(project(":config"))
    compileOnly(project(":onboarding"))
    minecraft("com.mojang:minecraft:26.1.2")
    compileOnly("net.fabricmc.fabric-api:fabric-api:0.150.0+26.1.2")
    compileOnly("net.fabricmc:fabric-loader:0.19.5")
}
