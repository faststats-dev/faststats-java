extra.set("moduleName", "dev.faststats.fabric.compat.v1_16_1")
extra.set("publishVersionSuffix", "mc1.16.1")

plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("faststats.fabric-compat")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.compileJava {
    options.release.set(17)
}

dependencies {
    minecraft(libs.minecraft.v1x16x1)
    mappings(loom.officialMojangMappings())

    compileOnly(libs.fabric.api.v1x16x1) // 1.16.1

    compileOnly(libs.fabric.loader)
}

dependencies {
    implementation(project(":onboarding:versions:1.16.1", "runtimeElements"))
}
