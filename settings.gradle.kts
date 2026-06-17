pluginManagement.repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases")
    gradlePluginPortal()
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

rootProject.name = "faststats-java"
include("bukkit")
include("bukkit:example-plugin")
include("bungeecord")
include("bungeecord:example-plugin")
include("config")
include("core")
include("core:example")
include("fabric")
include("fabric:example-mod")
include("hytale")
include("hytale:example-plugin")
include("minestom")
include("minestom:example-server")
include("neoforge")
include("neoforge:example-mod")
include("nukkit")
include("nukkit:example-plugin")
include("sponge")
include("sponge:example-plugin")
include("velocity")
include("velocity:example-plugin")
