val moduleName by extra("dev.faststats.bungee")

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(project(":core"))
    compileOnly("net.md-5:bungeecord-api:26.1-R0.1-SNAPSHOT")
}
