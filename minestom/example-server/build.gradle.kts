dependencies {
    implementation("net.minestom:minestom:2026.06.05-26.1.2")
    implementation(project(":minestom"))
}

tasks.shadowJar {
    // optionally relocate faststats
    relocate("dev.faststats", "com.example.utils.faststats")
}
