extra.set("moduleName", "dev.faststats.onboarding.minecraft.v1_19")

plugins {
    id("net.fabricmc.fabric-loom-remap")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.compileJava {
    options.release.set(17)
}

dependencies {
    minecraft(libs.minecraft.v1x19x3)
    mappings(loom.officialMojangMappings())
}
