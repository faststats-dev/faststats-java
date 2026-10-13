extra.set("moduleName", "dev.faststats.fabric.compat.v1_19_4")
extra.set("publishVersionSuffix", "mc1.20.3-1.20.4")

plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("faststats.fabric-compat")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.compileJava {
    options.release.set(17)
}

sourceSets.main {
    java.srcDir(project(":fabric:versions:1.19.4").file("src/main/java"))
    resources.srcDir(project(":fabric:versions:1.19.4").file("src/main/resources"))
}

dependencies {
    // minecraft(libs.minecraft.v1x20x3)
    minecraft(libs.minecraft.v1x20x4)
    mappings(loom.officialMojangMappings())

    // compileOnly(libs.fabric.api.v1x20x3) // 1.20.3
    compileOnly(libs.fabric.api.v1x20x4) // 1.20.4

    compileOnly(libs.fabric.loader)
}

dependencies {
    implementation(project(":onboarding:versions:1.20.3-1.20.4", "runtimeElements"))
}
