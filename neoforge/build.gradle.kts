val moduleName by extra("dev.faststats.neoforge")

repositories {
    maven("https://maven.neoforged.net/releases")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly("net.neoforged.fancymodloader:loader:10.0.36")
}
