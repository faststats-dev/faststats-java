val moduleName by extra("dev.faststats.neoforge")

plugins {
    id("net.neoforged.moddev") version "2.0.141"
}

neoForge {
    version = "26.1.2.76"
}

configurations.configureEach {
    resolutionStrategy.force("com.google.code.gson:gson:2.13.2")
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly("net.neoforged:bus:8.0.5")
}
