plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases")
}

fun plugin(plugin: Provider<PluginDependency>) = plugin.map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

dependencies {
    implementation(plugin(libs.plugins.fabric.loom))
    implementation(plugin(libs.plugins.moddev))
    implementation(plugin(libs.plugins.shadow))
}
