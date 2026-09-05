plugins {
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT"
    kotlin("jvm")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(25)
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("net.fabricmc:fabric-loader:0.19.3")
    implementation(project(":fabric:versions:26.1-26.3"))
    include(project(":fabric:versions:26.1-26.3"))
    minecraft("com.mojang:minecraft:26.2")
}
