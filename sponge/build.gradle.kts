val moduleName by extra("dev.faststats.sponge")

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    api(project(":core"))
    compileOnly("org.spongepowered:spongeapi:18.0.0-SNAPSHOT")
}
