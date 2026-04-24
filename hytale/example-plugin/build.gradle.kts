repositories {
    maven("https://maven.hytale.com/pre-release")
}

dependencies {
    compileOnly("com.hypixel.hytale:Server:2026.04.23-937872667")
    implementation(project(":hytale"))
}

tasks.shadowJar {
    // optionally relocate faststats
    relocate("dev.faststats", "com.example.utils.faststats")
}
