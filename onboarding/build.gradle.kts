extra.set("moduleName", "dev.faststats.onboarding")

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(17)
}

dependencies {
    api(project(":core"))
    compileOnlyApi("org.jspecify:jspecify:1.0.0")
}
