extra.set("moduleName", "dev.faststats.fabric.compat.v1_16_1")
extra.set("publishVersionSuffix", "mc1.16.1-1.17.1")

plugins {
    id("net.fabricmc.fabric-loom-remap")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(21)
}

dependencies {
    // minecraft("com.mojang:minecraft:1.16.1")
    // minecraft("com.mojang:minecraft:1.16.2")
    // minecraft("com.mojang:minecraft:1.16.3")
    // minecraft("com.mojang:minecraft:1.16.4")
    // minecraft("com.mojang:minecraft:1.16.5")
    // minecraft("com.mojang:minecraft:1.17")
    minecraft("com.mojang:minecraft:1.17.1")
    mappings(loom.officialMojangMappings())

    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.18.0+build.387-1.16.1") // 1.16.1
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.20.1+build.401-1.16") // 1.16.2
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.25.0+build.415-1.16") // 1.16.3
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.29.3+1.16") // 1.16.4
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.42.0+1.16") // 1.16.5
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.36.0+1.17") // 1.17
    compileOnly("net.fabricmc.fabric-api:fabric-api:0.46.1+1.17") // 1.17.1

    compileOnly("net.fabricmc:fabric-loader:0.19.3")
}
