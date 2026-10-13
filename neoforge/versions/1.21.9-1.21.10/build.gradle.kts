extra.set("moduleName", "dev.faststats.neoforge.compat.v1_21_9")
extra.set("publishVersionSuffix", "mc1.21.9-1.21.10")

plugins {
    id("faststats.neoforge-compat")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(21)
}

neoForge {
    // version = libs.versions.neoforge.v1x21x9.get() // 1.21.9
    version = libs.versions.neoforge.v1x21x10.get() // 1.21.10
}

dependencies {
    implementation(project(":onboarding:versions:1.21.9-1.21.10", "namedElements"))
}
