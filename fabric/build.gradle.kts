val moduleName by extra("dev.faststats.fabric")

plugins {
    id("fabric-loom") version ("1.15-SNAPSHOT")
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    mappings(loom.officialMojangMappings())
    minecraft("com.mojang:minecraft:1.21.11")
    modCompileOnly("net.fabricmc.fabric-api:fabric-api:0.139.4+1.21.11")
    modImplementation("net.fabricmc:fabric-loader:0.19.2")
}
