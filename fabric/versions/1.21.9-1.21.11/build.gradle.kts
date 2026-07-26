extra.set("moduleName", "dev.faststats.fabric.compat.v1_21_9")
extra.set("publishVersionSuffix", "mc1.21.9-1.21.11")

plugins {
    id("net.fabricmc.fabric-loom-remap")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(21)
}

dependencies {
    // minecraft("com.mojang:minecraft:1.21.9")
    // minecraft("com.mojang:minecraft:1.21.10")
    minecraft("com.mojang:minecraft:1.21.11")
    mappings(loom.officialMojangMappings())

    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.134.1+1.21.9") // 1.21.9
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.138.4+1.21.10") // 1.21.10
    compileOnly("net.fabricmc.fabric-api:fabric-api:0.141.4+1.21.11") // 1.21.11

    compileOnly("net.fabricmc:fabric-loader:0.19.3")
}
