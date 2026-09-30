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
published under stable Maven artifact IDs with Minecraft range suffixes:

```text
dev.faststats.metrics:fabric:<sdk-version>+mc26.1-26.2
dev.faststats.metrics:neoforge:<sdk-version>+mc26.1-26.2
```

Use `checkPlatformCompat` to compile all Fabric and NeoForge compatibility modules.

### Building everything

To compile and test all modules with the standard lifecycle, run:

```sh
./gradlew build
```

### Platform compatibility checks

Compile all published platform compatibility modules with:

```sh
./gradlew checkPlatformCompat
```
