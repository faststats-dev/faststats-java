extra.set("moduleName", "dev.faststats.fabric.compat.v26_1")
extra.set("publishVersionSuffix", "mc26.1-26.3")

plugins {
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(25)
}

dependencies {
    // minecraft("com.mojang:minecraft:26.1")
    // minecraft("com.mojang:minecraft:26.1.1")
    // minecraft("com.mojang:minecraft:26.1.2")
    // minecraft("com.mojang:minecraft:26.2")
    minecraft("com.mojang:minecraft:26.3-snapshot-1")

    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.153.0+26.1.2") // 26.1-26.1.2
    // compileOnly("net.fabricmc.fabric-api:fabric-api:0.153.0+26.2") // 26.2
    compileOnly("net.fabricmc.fabric-api:fabric-api:0.153.1+26.3") // 26.3

    compileOnly("net.fabricmc:fabric-loader:0.19.3")
}
