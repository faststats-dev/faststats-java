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

Library jars are written to each module's `build/libs` directory. Fabric, Forge and NeoForge compatibility artifacts are
published under stable Maven artifact IDs for every supported Minecraft version:

```text
dev.faststats.metrics:fabric:<sdk-version>+mc26.3
dev.faststats.metrics:forge:<sdk-version>+mc26.3
dev.faststats.metrics:neoforge:<sdk-version>+mc26.3
```

Each artifact only declares the Minecraft version it was built for in its `fabric.mod.json`, `mods.toml` or
`neoforge.mods.toml`.

See [CONTRIBUTING.md](CONTRIBUTING.md) for how the version-specific code is organized and how to add a Minecraft
version.

### Building everything

To compile and test all modules with the standard lifecycle, run:

```sh
./gradlew build
```

