# Configuration

GearSense creates `plugins/GearSense/config.yml` on first start. Run
`/gearsense reload` after editing it.

## Player defaults

Values under `defaults` initialize a player's setting until that player changes
it with a command. Player choices are stored persistently.

| Setting | Default | Meaning |
| --- | --- | --- |
| `enabled` | `false` | Automatically select tools for new players. |
| `refill` | `false` | Refill an emptied held stack from inventory. |
| `armor-replacement` | `true` | Equip matching armor after worn armor breaks. |
| `restore-slot` | `false` | Restore the original slot when sticky mode is off. |
| `preference` | `NONE` | Rank new tools by speed, Fortune, Silk Touch, or durability. |

## Server rules

| Setting | Default | Meaning |
| --- | --- | --- |
| `defaults.shift-bypass` | `true` | Sneaking temporarily prevents tool swaps. |
| `defaults.search-entire-inventory` | `false` | Search outside the hotbar and move tools into hand. |
| `defaults.durability-reserve` | `3` | Avoid selecting a new tool at or below this many uses. |
| `sticky-tool` | `true` | Keep a valid tool selected until it breaks. |
| `restore-delay-ticks` | `8` | Delay before restoring a slot when sticky mode is off. |
| `ignored-blocks` | See config | Blocks that never trigger a tool change. |

## Updater settings

The `updater` section controls asynchronous GitHub checks. `auto-download` is
off by default. See [Updater and releases](Updater%E2%80%90and%E2%80%90Releases) before enabling
it.

All material names in `ignored-blocks` must use uppercase Bukkit/Paper material
names. Message values support `&` color codes and can be set to `''` to hide
them.
