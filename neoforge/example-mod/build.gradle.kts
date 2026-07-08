plugins {
    id("net.neoforged.moddev") version "2.0.142"
    //kotlin("jvm")
}

neoForge {
    version = "26.1.2.76"
}

configurations.configureEach {
    resolutionStrategy.force("com.google.code.gson:gson:2.14.0")
}

dependencies {
    compileOnly(project(":neoforge:versions:26.1-26.2"))
    jarJar(project(":neoforge:versions:26.1-26.2"))
}
