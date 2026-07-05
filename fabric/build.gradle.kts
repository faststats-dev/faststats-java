extra.set("moduleName", "dev.faststats.fabric")

plugins {
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(21)
}

configurations.compileClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}

allprojects {
    if (project.name == "example-mod") return@allprojects
    apply { plugin("maven-publish") }
    extra.set("publishArtifactId", "fabric")
    extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/fabric")
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    minecraft("com.mojang:minecraft:26.1.2")
    compileOnly("net.fabricmc.fabric-api:fabric-api:0.154.0+26.3")
    compileOnly("net.fabricmc:fabric-loader:0.19.3")
}
