# Changelog

## 1.0.0

- Initial public release.
- Supports Minecraft 1.18.x through Paper 26.2 with one Java 17 JAR.
- Automatic context-aware tool selection with sticky-tool behavior.
- Keeps a valid tool selected until it breaks, then moves the next matching
  tool into the same hotbar slot.
- Fortune, Silk Touch, speed, and durability tool preferences.
- Configurable durability protection, inventory search, and ignored blocks.
- Automatic stack refill for blocks, food, and usable items.
- Automatic replacement of broken helmets, chestplates, leggings, and boots.
- Persistent player toggles for tools, refill, armor, restoration, locking,
  and preferences.
- Shift bypass, optional slot restoration, commands, permissions, and
  customizable messages.
- Fully commented configuration separating player defaults from server rules.
- Automatic migration of older `config.yml` files that preserves existing
  choices and unknown custom keys, adds new settings and comments, and creates
  a versioned backup before replacement.
- Asynchronous GitHub release updater with admin notifications, manual checks,
  verified one-time downloads, and optional automatic downloads.
- GitHub Actions release automation that publishes `1.0.0` first and then
  advances patch versions (`1.0.1`, `1.0.2`, and so on).
- Installation, configuration, updater, command, and release documentation for
  the GitHub Wiki.
