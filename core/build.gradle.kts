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
    compileOnlyApi("com.google.code.gson:gson:2.14.0")
    compileOnlyApi("org.jetbrains:annotations:26.1.0")
    compileOnlyApi("org.jspecify:jspecify:1.0.0")

    testImplementation(project(":config"))
    testImplementation("com.google.code.gson:gson:2.14.0")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
