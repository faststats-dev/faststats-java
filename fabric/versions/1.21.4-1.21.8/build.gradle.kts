extra.set("moduleName", "dev.faststats.fabric.compat.v1_19_4")
extra.set("publishVersionSuffix", "mc1.21.4-1.21.8")

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

sourceSets.main {
    java.srcDir(project(":fabric:versions:1.19.4").file("src/main/java"))
    resources.srcDir(project(":fabric:versions:1.19.4").file("src/main/resources"))
}

dependencies {
    // minecraft(libs.minecraft.v1x21x4)
    // minecraft(libs.minecraft.v1x21x5)
    // minecraft(libs.minecraft.v1x21x6)
    // minecraft(libs.minecraft.v1x21x7)
    minecraft(libs.minecraft.v1x21x8)
    mappings(loom.officialMojangMappings())

    // compileOnly(libs.fabric.api.v1x21x4) // 1.21.4
    // compileOnly(libs.fabric.api.v1x21x5) // 1.21.5
    // compileOnly(libs.fabric.api.v1x21x6) // 1.21.6
    // compileOnly(libs.fabric.api.v1x21x7) // 1.21.7
    compileOnly(libs.fabric.api.v1x21x8) // 1.21.8

    compileOnly(libs.fabric.loader)
}

dependencies {
    implementation(project(":onboarding:versions:1.21.4-1.21.8", "runtimeElements"))
}
