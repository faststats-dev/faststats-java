# FastStats Java

Documentation: https://docs.faststats.dev/java

## Building

Run Gradle from the repository root. The library modules use the standard Java lifecycle.

### Libraries

Use `build` for the reusable FastStats libraries:

```sh
./gradlew :core:build
./gradlew :config:build
./gradlew :bukkit:build
./gradlew :bungeecord:build
./gradlew :hytale:build
./gradlew :minestom:build
./gradlew :nukkit:build
./gradlew :sponge:build
./gradlew :velocity:build
```

Library jars are written to each module's `build/libs` directory. Fabric and NeoForge compatibility artifacts are
published under stable Maven artifact IDs for every supported Minecraft version:

```text
dev.faststats.metrics:fabric:<sdk-version>+mc26.1.2
dev.faststats.metrics:neoforge:<sdk-version>+mc26.1.2
```

Each artifact only declares the Minecraft version it was built for in its `fabric.mod.json` or `neoforge.mods.toml`.

The supported Minecraft versions are listed in `build-logic/src/main/kotlin/MinecraftVersions.kt`. Every version is
built from the sources in `<platform>/versions/<version>`, or from the closest older version that has a directory there.
A new Minecraft version that needs no code changes only has to be added to that list and the version catalog.

### Building everything

To compile and test all modules with the standard lifecycle, run:

```sh
./gradlew build
```

