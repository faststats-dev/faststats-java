extra.set("moduleName", "dev.faststats.fabric.compat.v1_16_1")
extra.set("publishVersionSuffix", "mc1.16.1-1.17.1")

plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("faststats.fabric-compat")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.compileJava {
    options.release.set(17)
}

dependencies {
    // minecraft(libs.minecraft.v1x16x1)
    // minecraft(libs.minecraft.v1x16x2)
    // minecraft(libs.minecraft.v1x16x3)
    // minecraft(libs.minecraft.v1x16x4)
    // minecraft(libs.minecraft.v1x16x5)
    // minecraft(libs.minecraft.v1x17)
    minecraft(libs.minecraft.v1x17x1)
    mappings(loom.officialMojangMappings())

    // compileOnly(libs.fabric.api.v1x16x1) // 1.16.1
    // compileOnly(libs.fabric.api.v1x16x2) // 1.16.2
    // compileOnly(libs.fabric.api.v1x16x3) // 1.16.3
    // compileOnly(libs.fabric.api.v1x16x4) // 1.16.4
    // compileOnly(libs.fabric.api.v1x16x5) // 1.16.5
    // compileOnly(libs.fabric.api.v1x17) // 1.17
    compileOnly(libs.fabric.api.v1x17x1) // 1.17.1

    compileOnly(libs.fabric.loader)
}

dependencies {
    implementation(project(":onboarding:versions:1.16.1-1.17.1", "namedElements"))
}
