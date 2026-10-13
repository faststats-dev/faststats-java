extra.set("moduleName", "dev.faststats.fabric.compat.v1_16_1")
extra.set("publishVersionSuffix", "mc1.17-1.17.1")

plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("faststats.fabric-compat")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.compileJava {
    options.release.set(17)
}

sourceSets.main {
    java.srcDir(project(":fabric:versions:1.16.1").file("src/main/java"))
    resources.srcDir(project(":fabric:versions:1.16.1").file("src/main/resources"))
}

dependencies {
    // minecraft(libs.minecraft.v1x17)
    minecraft(libs.minecraft.v1x17x1)
    mappings(loom.officialMojangMappings())

    // compileOnly(libs.fabric.api.v1x17) // 1.17
    compileOnly(libs.fabric.api.v1x17x1) // 1.17.1

    compileOnly(libs.fabric.loader)
}

dependencies {
    implementation(project(":onboarding:versions:1.17-1.18.2", "runtimeElements"))
}
