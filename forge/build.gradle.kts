val moduleName by extra("dev.faststats.forge")

repositories {
    maven("https://maven.minecraftforge.net/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly("net.minecraftforge:fmlloader:1.21-51.0.21")
    compileOnly("net.minecraftforge:forgespi:7.1.4")
}
