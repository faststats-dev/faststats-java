repositories {
    maven("https://maven.quiltmc.org/repository/release/")
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly("org.quiltmc:quilt-loader:0.29.1")
}
