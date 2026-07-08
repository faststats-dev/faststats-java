extra.set("moduleName", "dev.faststats.neoforge.compat.v26_1")
extra.set("publishVersionSuffix", "mc26.1-26.2")

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(25)
}

neoForge {
    // version = "26.1.0.19-beta" // 26.1
    // version = "26.1.1.15-beta" // 26.1.1
    // version = "26.1.2.76" // 26.1.2
    version = "26.2.0.7-beta" // 26.2
}

configurations.configureEach {
    resolutionStrategy.force("com.google.code.gson:gson:2.14.0")
}
