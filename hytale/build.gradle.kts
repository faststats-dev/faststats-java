extra.set("moduleName", "dev.faststats.hytale")
extra.set("publishArtifactId", "hytale")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/hytale")

plugins {
    id("maven-publish")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(25)
}

repositories {
    maven("https://maven.hytale.com/release")
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly("com.hypixel.hytale:Server:+")
}
