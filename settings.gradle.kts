pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
    }
}

plugins {
    id("faststats.project-structure")
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
