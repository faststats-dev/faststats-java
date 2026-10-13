extra.set("moduleName", "dev.faststats.fabric.compat.v1_21_9")
extra.set("publishVersionSuffix", "mc1.21.9-1.21.11")

plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("faststats.fabric-compat")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(21)
}

dependencies {
    // minecraft(libs.minecraft.v1x21x9)
    // minecraft(libs.minecraft.v1x21x10)
    minecraft(libs.minecraft.v1x21x11)
    mappings(loom.officialMojangMappings())

    // compileOnly(libs.fabric.api.v1x21x9) // 1.21.9
    // compileOnly(libs.fabric.api.v1x21x10) // 1.21.10
    compileOnly(libs.fabric.api.v1x21x11) // 1.21.11

    compileOnly(libs.fabric.loader)
}

dependencies {
    implementation(project(":onboarding:versions:1.21.9-1.21.11", "namedElements"))
}
