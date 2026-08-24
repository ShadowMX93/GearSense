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
| `/gearsense reload` | Reload configuration (admin). |

Aliases: `/gsense` and `/gs`.

## Build

```bash
mvn clean verify
```

The distributable JAR is written to `target/GearSense-1.0.0.jar`.

## Installation

1. Put `GearSense-1.0.0.jar` in the server's `plugins` directory.
2. Restart the server.
3. Run `/gearsense on` and optionally `/gearsense refill`.

Do not use Bukkit `/reload`; restart the server when replacing the JAR.
