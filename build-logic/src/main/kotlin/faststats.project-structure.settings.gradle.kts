pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
        gradlePluginPortal()
    }
}

rootProject.name = "faststats-java"

pluginManager.apply("faststats.core-projects")
pluginManager.apply("faststats.server-plugin-projects")
pluginManager.apply("faststats.mod-projects")
pluginManager.apply("faststats.standalone-projects")
