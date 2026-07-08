extra.set("moduleName", "dev.faststats.neoforge.compat.v1_20_6")
extra.set("publishVersionSuffix", "mc1.21-1.21.11")

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(25)
}

neoForge {
    // version = "20.6.139" // 1.20.6
    // version = "21.0.167" // 1.21
    // version = "21.1.234" // 1.21.1
    // version = "21.2.1-beta" // 1.21.2
    // version = "21.3.96" // 1.21.3
    // version = "21.4.157" // 1.21.4
    // version = "21.5.97" // 1.21.5
    // version = "21.6.20-beta" // 1.21.6
    // version = "21.7.25-beta" // 1.21.7
    version = "21.8.53" // 1.21.8
}

configurations.configureEach {
    resolutionStrategy.force("com.google.code.gson:gson:2.14.0")
}
