extra.set("moduleName", "dev.faststats.nukkit")
extra.set("publishArtifactId", "nukkit")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/nukkit")

plugins {
    id("maven-publish")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.compileJava {
    options.release.set(17)
}

repositories {
    maven("https://repo.opencollab.dev/maven-releases")
    maven("https://repo.opencollab.dev/maven-snapshots")
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly(libs.nukkit)
}
