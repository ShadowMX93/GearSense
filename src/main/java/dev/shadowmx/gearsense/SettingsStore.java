package dev.shadowmx.gearsense;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class SettingsStore {
    private final GearSensePlugin plugin;
    private final NamespacedKey enabledKey;
    private final NamespacedKey refillKey;
    private final NamespacedKey armorKey;
    private final NamespacedKey restoreKey;
    private final NamespacedKey lockedKey;
    private final NamespacedKey preferenceKey;

    public SettingsStore(GearSensePlugin plugin) {
        this.plugin = plugin;
        this.enabledKey = new NamespacedKey(plugin, "enabled");
        this.refillKey = new NamespacedKey(plugin, "refill");
        this.armorKey = new NamespacedKey(plugin, "armor_replacement");
        this.restoreKey = new NamespacedKey(plugin, "restore_slot");
        this.lockedKey = new NamespacedKey(plugin, "locked");
        this.preferenceKey = new NamespacedKey(plugin, "preference");
    }

    public PlayerSettings get(Player player) {
        FileConfiguration config = plugin.getConfig();
        PersistentDataContainer data = player.getPersistentDataContainer();
        boolean enabled = getBoolean(data, enabledKey, config.getBoolean("defaults.enabled", false));
        boolean refill = getBoolean(data, refillKey, config.getBoolean("defaults.refill", false));
        boolean armor = getBoolean(data, armorKey, config.getBoolean("defaults.armor-replacement", true));
        boolean restore = getBoolean(data, restoreKey, config.getBoolean("defaults.restore-slot", true));
        boolean locked = getBoolean(data, lockedKey, false);
        boolean fullInventory = config.getBoolean("defaults.search-entire-inventory", false);
        int reserve = Math.max(0, config.getInt("defaults.durability-reserve", 3));
        String preference = data.get(preferenceKey, PersistentDataType.STRING);
        if (preference == null) {
            preference = config.getString("defaults.preference", "NONE");
        }
        return new PlayerSettings(enabled, refill, armor, restore, locked, fullInventory, reserve, Preference.parse(preference));
    }

    public void save(Player player, PlayerSettings settings) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        setBoolean(data, enabledKey, settings.enabled());
        setBoolean(data, refillKey, settings.refill());
        setBoolean(data, armorKey, settings.armorReplacement());
        setBoolean(data, restoreKey, settings.restoreSlot());
        setBoolean(data, lockedKey, settings.locked());
        data.set(preferenceKey, PersistentDataType.STRING, settings.preference().name());
    }

    private static boolean getBoolean(PersistentDataContainer data, NamespacedKey key, boolean fallback) {
        Byte value = data.get(key, PersistentDataType.BYTE);
        return value == null ? fallback : value == (byte) 1;
    }

    private static void setBoolean(PersistentDataContainer data, NamespacedKey key, boolean value) {
        data.set(key, PersistentDataType.BYTE, value ? (byte) 1 : (byte) 0);
    }
}
