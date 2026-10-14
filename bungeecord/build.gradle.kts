extra.set("moduleName", "dev.faststats.bungee")
extra.set("publishArtifactId", "bungeecord")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/bungeecord")

plugins {
    id("maven-publish")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.compileJava {
    options.release.set(17)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly(libs.bungeecord.api)
}
