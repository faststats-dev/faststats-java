val moduleName by extra("dev.faststats.neoforge")

plugins {
    id("net.neoforged.moddev") version "2.0.141"
}

val neoForgeVersion = "26.1.2.76"
val busVersion = "8.0.5"

neoForge {
    version = neoForgeVersion
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly("net.neoforged:bus:$busVersion")
}
