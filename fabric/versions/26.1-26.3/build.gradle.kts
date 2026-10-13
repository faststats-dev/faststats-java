extra.set("moduleName", "dev.faststats.fabric.compat.v26_1")
extra.set("publishVersionSuffix", "mc26.1-26.3")

plugins {
    id("net.fabricmc.fabric-loom")
    id("faststats.fabric-compat")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(25)
}

dependencies {
    // minecraft(libs.minecraft.v26x1)
    // minecraft(libs.minecraft.v26x1x1)
    // minecraft(libs.minecraft.v26x1x2)
    // minecraft(libs.minecraft.v26x2)
    minecraft(libs.minecraft.v26x3)

    // compileOnly(libs.fabric.api.v26x1x2) // 26.1-26.1.2
    // compileOnly(libs.fabric.api.v26x2) // 26.2
    compileOnly(libs.fabric.api.v26x3) // 26.3

    compileOnly(libs.fabric.loader)
}

dependencies {
    implementation(project(":onboarding:versions:26.1-26.3", "runtimeElements"))
}
