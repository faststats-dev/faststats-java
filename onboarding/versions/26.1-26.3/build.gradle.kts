extra.set("moduleName", "dev.faststats.onboarding.minecraft.v26_1")

plugins {
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(25)
}

tasks.jar {
    manifest.attributes["MixinConfigs"] = "faststats-onboarding-v26_1.mixins.json"
}

dependencies {
    compileOnlyApi(project(":onboarding"))
    minecraft("com.mojang:minecraft:26.1.2")
    compileOnly("net.fabricmc:fabric-loader:0.19.3")
}
