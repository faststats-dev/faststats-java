extra.set("moduleName", "dev.faststats.onboarding.minecraft.v1_19_4")

plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.15-SNAPSHOT"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(17)
}

tasks.jar {
    manifest.attributes["MixinConfigs"] = "faststats-onboarding-modern.mixins.json"
}

dependencies {
    compileOnlyApi(project(":onboarding"))
    minecraft("com.mojang:minecraft:1.21.8")
    mappings(loom.officialMojangMappings())
    compileOnly("net.fabricmc:fabric-loader:0.19.3")
    compileOnly("net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7")
}
