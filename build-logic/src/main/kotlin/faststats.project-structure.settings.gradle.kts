pluginManagement.repositories {
    gradlePluginPortal()
}

rootProject.name = "faststats-java"

pluginManager.apply("faststats.core-projects")
pluginManager.apply("faststats.server-plugin-projects")
pluginManager.apply("faststats.mod-projects")
