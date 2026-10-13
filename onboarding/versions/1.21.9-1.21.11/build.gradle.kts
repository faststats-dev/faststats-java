extra.set("moduleName", "dev.faststats.onboarding.minecraft.v1_21_9")

plugins {
    id("net.fabricmc.fabric-loom-remap")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(21)
}

sourceSets.main {
    java.srcDir(project(":onboarding:versions:1.19.4-1.21.8").file("src/main/java/dev"))
    resources.srcDir(project(":onboarding:versions:1.19.4-1.21.8").file("src/main/resources"))
}

dependencies {
    minecraft(libs.minecraft.v1x21x11)
    mappings(loom.officialMojangMappings())
}
