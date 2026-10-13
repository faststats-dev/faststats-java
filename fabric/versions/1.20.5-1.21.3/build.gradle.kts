extra.set("moduleName", "dev.faststats.fabric.compat.v1_19_4")
extra.set("publishVersionSuffix", "mc1.20.5-1.21.3")

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
    // minecraft(libs.minecraft.v1x20x5)
    // minecraft(libs.minecraft.v1x20x6)
    // minecraft(libs.minecraft.v1x21)
    // minecraft(libs.minecraft.v1x21x1)
    // minecraft(libs.minecraft.v1x21x2)
    minecraft(libs.minecraft.v1x21x3)
    mappings(loom.officialMojangMappings())

    // compileOnly(libs.fabric.api.v1x20x5) // 1.20.5
    // compileOnly(libs.fabric.api.v1x20x6) // 1.20.6
    // compileOnly(libs.fabric.api.v1x21) // 1.21
    // compileOnly(libs.fabric.api.v1x21x1) // 1.21.1
    // compileOnly(libs.fabric.api.v1x21x2) // 1.21.2
    compileOnly(libs.fabric.api.v1x21x3) // 1.21.3

    compileOnly(libs.fabric.loader)
}

dependencies {
    implementation(project(":onboarding:versions:1.20.5-1.21.3", "runtimeElements"))
}
