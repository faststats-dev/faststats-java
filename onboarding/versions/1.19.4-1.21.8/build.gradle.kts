extra.set("moduleName", "dev.faststats.onboarding.minecraft.v1_19_4")

plugins {
    id("net.fabricmc.fabric-loom-remap")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(17)
}

dependencies {
    minecraft(libs.minecraft.v1x21x8)
    mappings(loom.officialMojangMappings())
}
