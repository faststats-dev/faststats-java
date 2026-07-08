extra.set("moduleName", "dev.faststats.fabric.compat.v1_18")
extra.set("publishVersionSuffix", "mc1.18-1.21.8")

plugins {
    id("net.fabricmc.fabric-loom-remap")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(21)
}

dependencies {
    // minecraft("com.mojang:minecraft:1.18")
    // minecraft("com.mojang:minecraft:1.18.1")
    // minecraft("com.mojang:minecraft:1.18.2")
    // minecraft("com.mojang:minecraft:1.19")
    // minecraft("com.mojang:minecraft:1.19.1")
    // minecraft("com.mojang:minecraft:1.19.2")
    // minecraft("com.mojang:minecraft:1.19.3")
    // minecraft("com.mojang:minecraft:1.19.4")
    // minecraft("com.mojang:minecraft:1.20")
    // minecraft("com.mojang:minecraft:1.20.1")
    // minecraft("com.mojang:minecraft:1.20.2")
    // minecraft("com.mojang:minecraft:1.20.3")
    // minecraft("com.mojang:minecraft:1.20.4")
    // minecraft("com.mojang:minecraft:1.20.5")
    // minecraft("com.mojang:minecraft:1.20.6")
    // minecraft("com.mojang:minecraft:1.21")
    // minecraft("com.mojang:minecraft:1.21.1")
    // minecraft("com.mojang:minecraft:1.21.2")
    // minecraft("com.mojang:minecraft:1.21.3")
    // minecraft("com.mojang:minecraft:1.21.4")
    // minecraft("com.mojang:minecraft:1.21.5")
    // minecraft("com.mojang:minecraft:1.21.6")
    // minecraft("com.mojang:minecraft:1.21.7")
    minecraft("com.mojang:minecraft:1.21.8")
    mappings(loom.officialMojangMappings())

    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.44.0+1.18") // 1.18
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.46.6+1.18") // 1.18.1
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.77.0+1.18.2") // 1.18.2
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.58.0+1.19") // 1.19
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.58.5+1.19.1") // 1.19.1
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.77.0+1.19.2") // 1.19.2
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.76.1+1.19.3") // 1.19.3
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.87.2+1.19.4") // 1.19.4
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.83.0+1.20") // 1.20
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.92.9+1.20.1") // 1.20.1
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.91.6+1.20.2") // 1.20.2
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.91.1+1.20.3") // 1.20.3
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.97.3+1.20.4") // 1.20.4
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.97.8+1.20.5") // 1.20.5
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.100.8+1.20.6") // 1.20.6
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.102.0+1.21") // 1.21
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.116.13+1.21") // 1.21.1
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.106.1+1.21.2") // 1.21.2
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.114.1+1.21.3") // 1.21.3
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.119.4+1.21.4") // 1.21.4
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.128.2+1.21.5") // 1.21.5
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.128.2+1.21.6") // 1.21.6
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.129.0+1.21.7") // 1.21.7
    compileOnly("net.fabricmc.fabric-api:fabric-api:0.136.1+1.21.8") // 1.21.8

    compileOnly("net.fabricmc:fabric-loader:0.19.3")
}
