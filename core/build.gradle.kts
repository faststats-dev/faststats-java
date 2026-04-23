dependencies {
    compileOnlyApi("com.google.code.gson:gson:2.14.0")
    compileOnlyApi("org.jetbrains:annotations:26.1.0")
    compileOnlyApi("org.jspecify:jspecify:1.0.0")

    testImplementation("com.google.code.gson:gson:2.14.0")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(platform("org.junit:junit-bom:6.1.0-M1"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
