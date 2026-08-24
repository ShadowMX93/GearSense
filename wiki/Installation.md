# Installation

1. Download `GearSense-X.Y.Z.jar` from the repository's **Releases** page.
2. Put the JAR in the Minecraft server's `plugins` directory.
3. Start or fully restart the server.
4. Confirm that `GearSense 1.0.0 enabled` appears in the console.
5. Players can run `/gearsense on` to enable automatic tool selection.

Do not use Bukkit `/reload` to replace the plugin JAR. Stop and restart the
server so tasks, listeners, and the installed JAR are cleanly reloaded.

## Java requirements

GearSense itself is compiled for Java 17. The Minecraft server may require a
newer runtime: for example, Paper 26.2 requires Java 25 or later.

## Building from source

On Windows:

```bat
gradle.bat clean build
```

On macOS or Linux:

```bash
./gradlew clean build
```

The Gradle output is `build/libs/GearSense-X.Y.Z.jar`. Maven remains supported
with `mvn clean verify`, which writes the JAR to `target/`.
