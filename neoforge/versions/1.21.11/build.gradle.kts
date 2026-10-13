extra.set("moduleName", "dev.faststats.neoforge.compat.v1_21_9")
extra.set("publishVersionSuffix", "mc1.21.11")

plugins {
    id("faststats.neoforge-compat")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(21)
}

sourceSets.main {
    java.srcDir(project(":neoforge:versions:1.21.9-1.21.10").file("src/main/java"))
    resources.srcDir(project(":neoforge:versions:1.21.9-1.21.10").file("src/main/resources"))
}

neoForge {
    version = libs.versions.neoforge.v1x21x11.get() // 1.21.11
}

dependencies {
    implementation(project(":onboarding:versions:1.21.11", "namedElements"))
}
