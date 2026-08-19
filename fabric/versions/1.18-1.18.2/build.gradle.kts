extra.set("moduleName", "dev.faststats.fabric.compat.v1_18")
extra.set("publishVersionSuffix", "mc1.18-1.18.2")

plugins {
    id("net.fabricmc.fabric-loom-remap")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.compileJava {
    options.release.set(17)
}

sourceSets.main {
    java.srcDir(project(":fabric:versions:1.18-1.21.8").file("src/main/java"));
    resources.srcDir(project(":fabric:versions:1.18-1.21.8").file("src/main/resources"))
}

dependencies {
    minecraft("com.mojang:minecraft:1.18.2");
    mappings(loom.officialMojangMappings());
    compileOnly("net.fabricmc.fabric-api:fabric-api:0.77.0+1.18.2");
    compileOnly("net.fabricmc:fabric-loader:0.19.3")
}
