extra.set("moduleName", "dev.faststats.onboarding.minecraft.v1_21_9")

plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.15-SNAPSHOT"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(21)
}

sourceSets.main {
    java.srcDir(project(":onboarding:versions:1.19.4-1.21.8").file("src/main/java/dev"))
    resources.srcDir(project(":onboarding:versions:1.19.4-1.21.8").file("src/main/resources"))
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.11")
    mappings(loom.officialMojangMappings())
}
