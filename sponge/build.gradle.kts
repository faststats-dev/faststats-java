val moduleName by extra("dev.faststats.sponge")

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly("org.spongepowered:spongeapi:8.3.0-SNAPSHOT")
}
