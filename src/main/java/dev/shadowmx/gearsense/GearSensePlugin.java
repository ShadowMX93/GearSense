package dev.shadowmx.gearsense;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

public final class GearSensePlugin extends JavaPlugin {
    private Set<Material> ignoredBlocks = Collections.emptySet();
    private int restoreDelayTicks;
    private boolean shiftBypass;
    private boolean stickyTool;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocalConfiguration();

        SettingsStore store = new SettingsStore(this);
        GearListener listener = new GearListener(this, store, new ToolSelector());
        getServer().getPluginManager().registerEvents(listener, this);

        PluginCommand command = getCommand("gearsense");
        if (command == null) {
            throw new IllegalStateException("Command 'gearsense' is missing from plugin.yml");
        }
        GearCommand executor = new GearCommand(this, store, listener);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        getLogger().info("GearSense " + getDescription().getVersion() + " enabled on "
                + getServer().getName() + " " + getServer().getBukkitVersion()
                + " (supported range: 1.18.x - 26.2).");
    }

    public void reloadLocalConfiguration() {
        reloadConfig();
        restoreDelayTicks = Math.max(0, getConfig().getInt("restore-delay-ticks", 8));
        shiftBypass = getConfig().getBoolean("defaults.shift-bypass", true);
        stickyTool = getConfig().getBoolean("sticky-tool", true);
        EnumSet<Material> parsed = EnumSet.noneOf(Material.class);
        for (String name : getConfig().getStringList("ignored-blocks")) {
            Material material = Material.matchMaterial(name);
            if (material == null || !material.isBlock()) {
                getLogger().log(Level.WARNING, "Ignoring unknown block in ignored-blocks: {0}", name);
            } else {
                parsed.add(material);
            }
        }
        ignoredBlocks = Collections.unmodifiableSet(parsed);
    }

    public Set<Material> getIgnoredBlocks() {
        return ignoredBlocks;
    }

    public int getRestoreDelayTicks() {
        return restoreDelayTicks;
    }

    public boolean isShiftBypass() {
        return shiftBypass;
    }

    public boolean isStickyTool() {
        return stickyTool;
    }

    public String message(String key) {
        String prefix = getConfig().getString("messages.prefix", "&8[&bGearSense&8] ");
        String value = getConfig().getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes('&', prefix + value);
    }
}
