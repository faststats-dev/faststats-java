extra.set("moduleName", "dev.faststats.forge")
extra.set("publishArtifactId", "forge")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/forge")

plugins {
    id("faststats.shadow-conventions")
    id("maven-publish")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(21)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":config"))
    compileOnlyApi(project(":onboarding"))
    compileOnly(libs.gson)
    compileOnly(libs.slf4j.api)
}

subprojects {
    if (parent?.path == ":forge:versions") pluginManager.apply("faststats.forge-compat")
}
