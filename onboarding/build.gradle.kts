extra.set("moduleName", "dev.faststats.onboarding")

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(17)
}

dependencies {
    compileOnlyApi("org.jspecify:jspecify:1.0.0")
}
