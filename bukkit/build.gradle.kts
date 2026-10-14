extra.set("publishArtifactId", "bukkit")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/bukkit")

plugins {
    id("maven-publish")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(17)
}

configurations.compileClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly(libs.paper.api)
}
