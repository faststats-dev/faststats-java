// compiles the sources of the version's variant against that exact Minecraft version
plugins {
    id("java")
}

val minecraft = minecraftVersion
val variant = variantDirectory()

extra.set("minecraftVariant", minecraftVariant().name)

java.toolchain.languageVersion = JavaLanguageVersion.of(minecraft.java)

tasks.compileJava {
    options.release.set(minecraft.java)
}

// the project of the variant's own version already uses its sources
if (variant != projectDir) sourceSets.main {
    java.srcDir(variant.resolve("src/main/java"))
    resources.srcDir(variant.resolve("src/main/resources"))
}
