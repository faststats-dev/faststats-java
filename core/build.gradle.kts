extra.set("publishArtifactId", "core")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java") // todo: add dedicated docs

plugins {
    id("maven-publish")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.compileJava {
    options.release.set(17)
}

dependencies {
    compileOnlyApi(libs.gson)
    compileOnlyApi(libs.jetbrains.annotations)
    compileOnlyApi(libs.jspecify)

    testImplementation(project(":config"))
    testImplementation(libs.gson)
}
