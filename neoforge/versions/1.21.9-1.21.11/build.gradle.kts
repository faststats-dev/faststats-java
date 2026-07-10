extra.set("moduleName", "dev.faststats.neoforge.compat.v1_21_9")
extra.set("publishVersionSuffix", "mc1.21.9-1.21.11")

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(21)
}

neoForge {
    // version = "21.9.16-beta" // 1.21.9
    // version = "21.10.64" // 1.21.10
    version = "21.11.42" // 1.21.11
}

configurations.configureEach {
    resolutionStrategy.force("com.google.code.gson:gson:2.14.0")
}
