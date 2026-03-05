val moduleName by extra("dev.faststats.hytale")

repositories {
    maven("https://maven.hytale.com/pre-release")
}

dependencies {
    api(project(":core"))
    compileOnly("com.hypixel.hytale:Server:2026.03.05-9fdc5985d")
}
