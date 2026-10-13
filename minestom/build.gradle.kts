extra.set("publishArtifactId", "minestom")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/minestom")

plugins {
    id("maven-publish")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(25)
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly(libs.minestom) {
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        }
    }
}
