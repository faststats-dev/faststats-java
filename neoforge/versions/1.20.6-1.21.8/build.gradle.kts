extra.set("moduleName", "dev.faststats.neoforge.compat.v1_20_6")
extra.set("publishVersionSuffix", "mc1.20.6-1.21.8")

plugins {
    id("faststats.neoforge-compat")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(21)
}

neoForge {
    // version = libs.versions.neoforge.v1x20x6.get() // 1.20.6
    // version = libs.versions.neoforge.v1x21.get() // 1.21
    // version = libs.versions.neoforge.v1x21x1.get() // 1.21.1
    // version = libs.versions.neoforge.v1x21x2.get() // 1.21.2
    // version = libs.versions.neoforge.v1x21x3.get() // 1.21.3
    // version = libs.versions.neoforge.v1x21x4.get() // 1.21.4
    // version = libs.versions.neoforge.v1x21x5.get() // 1.21.5
    // version = libs.versions.neoforge.v1x21x6.get() // 1.21.6
    // version = libs.versions.neoforge.v1x21x7.get() // 1.21.7
    version = libs.versions.neoforge.v1x21x8.get() // 1.21.8
}

dependencies {
    implementation(project(":onboarding:versions:1.19.4-1.21.8", "namedElements"))
}
