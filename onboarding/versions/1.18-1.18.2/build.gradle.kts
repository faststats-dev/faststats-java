extra.set("moduleName", "dev.faststats.onboarding.minecraft.v1_18")

plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.15-SNAPSHOT"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.compileJava {
    options.release.set(17)
}

tasks.jar {
    manifest.attributes["MixinConfigs"] = "faststats-onboarding-legacy.mixins.json"
}

sourceSets.main {
    java.srcDir(project(":onboarding:versions:1.16.1-1.17.1").file("src/main/java"));
    resources.srcDir(project(":onboarding:versions:1.16.1-1.17.1").file("src/main/resources"))
}
dependencies {
    compileOnlyApi(project(":onboarding"));
    minecraft("com.mojang:minecraft:1.18.2")
    mappings(loom.officialMojangMappings());
    compileOnly("net.fabricmc:fabric-loader:0.19.3")
    compileOnly("net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7")
}
