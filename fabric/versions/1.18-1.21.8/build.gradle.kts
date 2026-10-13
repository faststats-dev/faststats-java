extra.set("moduleName", "dev.faststats.fabric.compat.v1_18")
extra.set("publishVersionSuffix", "mc1.19.4-1.21.8")

plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("faststats.fabric-compat")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.compileJava {
    options.release.set(17)
}

configurations.compileClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
}

dependencies {
    // minecraft(libs.minecraft.v1x18)
    // minecraft(libs.minecraft.v1x18x1)
    // minecraft(libs.minecraft.v1x18x2)
    // minecraft(libs.minecraft.v1x19)
    // minecraft(libs.minecraft.v1x19x1)
    // minecraft(libs.minecraft.v1x19x2)
    // minecraft(libs.minecraft.v1x19x3)
    // minecraft(libs.minecraft.v1x19x4)
    // minecraft(libs.minecraft.v1x20)
    // minecraft(libs.minecraft.v1x20x1)
    // minecraft(libs.minecraft.v1x20x2)
    // minecraft(libs.minecraft.v1x20x3)
    // minecraft(libs.minecraft.v1x20x4)
    // minecraft(libs.minecraft.v1x20x5)
    // minecraft(libs.minecraft.v1x20x6)
    // minecraft(libs.minecraft.v1x21)
    // minecraft(libs.minecraft.v1x21x1)
    // minecraft(libs.minecraft.v1x21x2)
    // minecraft(libs.minecraft.v1x21x3)
    // minecraft(libs.minecraft.v1x21x4)
    // minecraft(libs.minecraft.v1x21x5)
    // minecraft(libs.minecraft.v1x21x6)
    // minecraft(libs.minecraft.v1x21x7)
    minecraft(libs.minecraft.v1x21x8)
    mappings(loom.officialMojangMappings())

    // compileOnly(libs.fabric.api.v1x18) // 1.18
    // compileOnly(libs.fabric.api.v1x18x1) // 1.18.1
    // compileOnly(libs.fabric.api.v1x18x2) // 1.18.2
    // compileOnly(libs.fabric.api.v1x19) // 1.19
    // compileOnly(libs.fabric.api.v1x19x1) // 1.19.1
    // compileOnly(libs.fabric.api.v1x19x2) // 1.19.2
    // compileOnly(libs.fabric.api.v1x19x3) // 1.19.3
    // compileOnly(libs.fabric.api.v1x19x4) // 1.19.4
    // compileOnly(libs.fabric.api.v1x20) // 1.20
    // compileOnly(libs.fabric.api.v1x20x1) // 1.20.1
    // compileOnly(libs.fabric.api.v1x20x2) // 1.20.2
    // compileOnly(libs.fabric.api.v1x20x3) // 1.20.3
    // compileOnly(libs.fabric.api.v1x20x4) // 1.20.4
    // compileOnly(libs.fabric.api.v1x20x5) // 1.20.5
    // compileOnly(libs.fabric.api.v1x20x6) // 1.20.6
    // compileOnly(libs.fabric.api.v1x21) // 1.21
    // compileOnly(libs.fabric.api.v1x21x1) // 1.21.1
    // compileOnly(libs.fabric.api.v1x21x2) // 1.21.2
    // compileOnly(libs.fabric.api.v1x21x3) // 1.21.3
    // compileOnly(libs.fabric.api.v1x21x4) // 1.21.4
    // compileOnly(libs.fabric.api.v1x21x5) // 1.21.5
    // compileOnly(libs.fabric.api.v1x21x6) // 1.21.6
    // compileOnly(libs.fabric.api.v1x21x7) // 1.21.7
    compileOnly(libs.fabric.api.v1x21x8) // 1.21.8

    compileOnly(libs.fabric.loader)
}

dependencies {
    implementation(project(":onboarding:versions:1.19.4-1.21.8", "namedElements"))
}
