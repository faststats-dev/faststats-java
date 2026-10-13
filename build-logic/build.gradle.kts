plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases")
}

fun plugin(id: String, version: String) = "$id:$id.gradle.plugin:$version"

dependencies {
    implementation(plugin("com.gradleup.shadow", "9.6.1"))
    implementation(plugin("net.fabricmc.fabric-loom", "1.15-SNAPSHOT"))
    implementation(plugin("net.neoforged.moddev", "2.0.148"))
}
