extra.set("moduleName", "dev.faststats.onboarding.minecraft.v26_1")

plugins {
    id("net.fabricmc.fabric-loom")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(25)
}

dependencies {
    minecraft("com.mojang:minecraft:26.1.2")
}
