extra.set("moduleName", "dev.faststats.onboarding.minecraft.v1_18")

plugins {
    id("net.fabricmc.fabric-loom-remap")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.compileJava {
    options.release.set(17)
}

sourceSets.main {
    java.srcDir(project(":onboarding:versions:1.16.1-1.17.1").file("src/main/java/dev"))
    resources.srcDir(project(":onboarding:versions:1.16.1-1.17.1").file("src/main/resources"))
}

dependencies {
    minecraft(libs.minecraft.v1x18x2)
    mappings(loom.officialMojangMappings())
}
