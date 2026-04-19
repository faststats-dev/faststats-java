repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

tasks.compileJava {
    options.release.set(17)
}

configurations.compileClasspath {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}
