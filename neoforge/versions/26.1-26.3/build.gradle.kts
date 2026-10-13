extra.set("moduleName", "dev.faststats.neoforge.compat.v26_1")
extra.set("publishVersionSuffix", "mc26.1-26.3")

plugins {
    id("faststats.neoforge-compat")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(25)
}

neoForge {
    // version = libs.versions.neoforge.v26x1.get() // 26.1
    // version = libs.versions.neoforge.v26x1x1.get() // 26.1.1
    // version = libs.versions.neoforge.v26x1x2.get() // 26.1.2
    // version = libs.versions.neoforge.v26x2.get() // 26.2
    version = libs.versions.neoforge.v26x3.get() // 26.3
}

dependencies {
    implementation(project(":onboarding:versions:26.1-26.3", "runtimeElements"))
}
