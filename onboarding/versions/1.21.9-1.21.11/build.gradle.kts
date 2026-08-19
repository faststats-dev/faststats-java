extra.set("moduleName", "dev.faststats.onboarding.minecraft.v1_21_9")

plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.15-SNAPSHOT"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(21)
}

tasks.jar {
    manifest.attributes["MixinConfigs"] = "faststats-onboarding-modern.mixins.json"
}

sourceSets.main {
    java.srcDir(project(":onboarding:versions:1.19.4-1.21.8").file("src/main/java"))
}

tasks.processResources {
    from(project(":onboarding:versions:1.19.4-1.21.8").file("src/main/resources"))
}

dependencies {
    compileOnlyApi(project(":onboarding"))
    minecraft("com.mojang:minecraft:1.21.11")
    mappings(loom.officialMojangMappings())
    compileOnly("net.fabricmc:fabric-loader:0.19.3")
    compileOnly("net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7")
}
