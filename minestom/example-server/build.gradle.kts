plugins {
    id("com.gradleup.shadow")
    kotlin("jvm")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(25)
}

dependencies {
    implementation("net.minestom:minestom:2026.08.07-26.2")
    implementation(project(":minestom"))
}

tasks.shadowJar {
    // optionally relocate faststats
    relocate("dev.faststats", "com.example.utils.faststats")
}
