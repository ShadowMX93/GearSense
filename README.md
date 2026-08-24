# GearSense

GearSense is a context-aware automatic tool selector and hotbar refiller for
Minecraft servers. It is an independent, clean-room implementation.

## Compatibility

- Paper, Purpur, and Spigot
- Minecraft **1.18.x through 26.2**
- Java 17 bytecode (the server may require a newer Java runtime)
- One JAR for every supported version

GearSense compiles against the Spigot 1.18.2 API, emits Java 17 bytecode, uses
`api-version: 1.18`, and avoids versioned NMS/CraftBukkit classes. Newer servers,
including Paper 26.2, can therefore load the same binary.

| Server | Required Java runtime |
| --- | --- |
| Minecraft 1.18.x–1.20.4 | Java 17+ |
| Later Minecraft versions | Use the Java version required by that Paper build |
| Paper 26.2 | Java 25+ |

The release JAR was startup-tested at both ends of the supported range:
Paper 1.18.2 build 388 and Paper 26.2 build 117.

## Features

- Selects the best safe tool before block damage begins.
- Keeps using the current valid tool until it breaks instead of bouncing
  between duplicate tools after every block.
- Moves the next matching tool into the same hotbar slot when one breaks.
- Equips the next matching helmet, chestplate, leggings, or boots from the
  inventory when worn armor breaks.
- Checks whether the server considers an item a preferred tool.
- Scores tool material, Efficiency, remaining durability, and block affinity.
- Fortune, Silk Touch, speed, and durability preferences.
- Protects tools at or below a configurable durability reserve.
- Optional legacy slot restoration when sticky-tool mode is disabled.
- Sneak bypass and persistent per-player slot lock.
- Optional whole-inventory search.
- Automatic hotbar refill.
- Persistent per-player settings stored in Bukkit PDC.
- Configurable ignored blocks and messages.
- Automatic versioned config migration that preserves existing choices and
  creates a backup before adding newly documented options.
- Asynchronous GitHub release checks with optional verified auto-downloads.
- No NMS, packets, or external runtime dependencies.

## Commands

| Command | Description |
| --- | --- |
| `/gearsense on` | Enable automatic tool selection. |
| `/gearsense off` | Disable automatic tool selection. |
| `/gearsense status` | Display the player's settings. |
| `/gearsense refill` | Toggle automatic refill. |
| `/gearsense armor` | Toggle automatic armor replacement. |
| `/gearsense restore` | Toggle restoration of the previous slot. |
| `/gearsense lock` | Lock/unlock the selected slot. |
| `/gearsense prefer <mode>` | Choose none, speed, fortune, silk-touch, or durability. |
| `/gearsense update status` | Show the result of the latest release check (admin). |
| `/gearsense update check` | Check GitHub Releases immediately (admin). |
| `/gearsense update download` | Check and download a newer JAR for the next restart (admin). |
| `/gearsense reload` | Reload configuration (admin). |

Aliases: `/gsense` and `/gs`.

## Build

Gradle (Windows):

```bat
gradle.bat clean build
```

Gradle (macOS/Linux):

```bash
./gradlew clean build
```

The Gradle JAR is written to `build/libs/GearSense-1.0.0.jar`.

Maven is also supported:

```bash
mvn clean verify
```

The Maven JAR is written to `target/GearSense-1.0.0.jar`.

## Installation

1. Put `GearSense-1.0.0.jar` in the server's `plugins` directory.
2. Restart the server.
3. Run `/gearsense on` and optionally `/gearsense refill`.

Do not use Bukkit `/reload`; restart the server when replacing the JAR.

When an update introduces configuration options, GearSense upgrades the file
on startup. Existing values and unknown custom keys are kept, the latest
comments are added, and the previous file is saved beside it as a versioned
`config.yml.vN.backup` file.

## Updater

The updater checks only the official
[`ShadowMX93/GearSense`](https://github.com/ShadowMX93/GearSense/releases)
GitHub Releases feed. Checks run asynchronously. By default GearSense only
notifies admins; set `updater.auto-download: true` to download a newer release
JAR to `plugins/update`. Bukkit installs that JAR on the next full restart.

Downloaded files are accepted only when they come from the official GitHub
release URL and contain a valid GearSense `plugin.yml` with the expected
release version.

## Releases

Open **Actions → Release GearSense → Run workflow**. If the current project
version has not been released, the workflow publishes it. Otherwise, it
automatically increments the patch version (`1.0.1`, `1.0.2`, and so on),
updates versioned files, runs all tests, commits the bump, creates the tag,
builds the JAR, and publishes it as a GitHub Release.

If a release was deleted but its tag remains, a manual run republishes the
current version and safely moves that tag to the current release commit.

Pushing a matching `vX.Y.Z` tag also builds and publishes that version. See
the [GitHub Wiki](https://github.com/ShadowMX93/GearSense/wiki) for complete
setup and operation details.
