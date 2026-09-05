extra.set("moduleName", "dev.faststats.fabric.compat.v1_18")
extra.set("publishVersionSuffix", "mc1.19-1.19.3")

plugins {
    id("net.fabricmc.fabric-loom-remap")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.compileJava {
    options.release.set(17)
}

sourceSets.main {
    java.srcDir(project(":fabric:versions:1.18-1.21.8").file("src/main/java"))
    resources.srcDir(project(":fabric:versions:1.18-1.21.8").file("src/main/resources"))
}

dependencies {
    minecraft("com.mojang:minecraft:1.19.3")
    mappings(loom.officialMojangMappings())
    compileOnly("net.fabricmc.fabric-api:fabric-api:0.76.1+1.19.3")
    compileOnly("net.fabricmc:fabric-loader:0.19.3")
}

dependencies {
    "bundled"(project(mapOf(
        "path" to ":onboarding:versions:1.19-1.19.3",
        "configuration" to "namedElements"
    )))
}
