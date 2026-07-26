extra.set("moduleName", "dev.faststats.sponge")
extra.set("publishArtifactId", "sponge")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/sponge")

plugins {
    id("maven-publish")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.compileJava {
    options.release.set(17)
}

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    api(project(":core"))
    compileOnly("org.spongepowered:spongeapi:20.0.0-SNAPSHOT")
}
