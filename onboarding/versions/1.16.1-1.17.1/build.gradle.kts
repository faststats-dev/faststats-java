extra.set("moduleName", "dev.faststats.onboarding.minecraft.v1_16_1")

plugins {
    id("net.fabricmc.fabric-loom-remap")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.compileJava {
    options.release.set(17)
}

dependencies {
    minecraft("com.mojang:minecraft:1.17.1")
    mappings(loom.officialMojangMappings())
}
