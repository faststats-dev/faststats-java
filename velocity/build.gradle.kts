extra.set("publishArtifactId", "velocity")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/velocity")

plugins {
    id("maven-publish")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(21)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly("com.velocitypowered:velocity-api:3.5.1")
}
