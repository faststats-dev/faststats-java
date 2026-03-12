repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:4.0.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:4.0.0-SNAPSHOT")
    implementation(project(":velocity"))
}

tasks.shadowJar {
    // optionally relocate faststats
    relocate("dev.faststats", "com.example.utils.faststats")
}
