extra.set("moduleName", "dev.faststats.onboarding.minecraft.v1_20_5")

plugins {
    id("net.fabricmc.fabric-loom-remap")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(21)
}

dependencies {
    minecraft(libs.minecraft.v1x20x5)
    mappings(loom.officialMojangMappings())
}
