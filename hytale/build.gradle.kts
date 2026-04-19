val moduleName by extra("dev.faststats.hytale")

repositories {
    maven("https://maven.hytale.com/pre-release")
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly("com.hypixel.hytale:Server:2026.05.07-5efa15f6d")
}
