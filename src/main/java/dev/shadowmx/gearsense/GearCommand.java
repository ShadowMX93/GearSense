package dev.shadowmx.gearsense;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class GearCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ROOT = List.of("on", "off", "status", "refill", "armor", "restore", "lock", "prefer", "reload");
    private static final List<String> PREFERENCES = List.of("none", "speed", "fortune", "silk-touch", "durability");
    private final GearSensePlugin plugin;
    private final SettingsStore store;
    private final GearListener listener;

    public GearCommand(GearSensePlugin plugin, SettingsStore store, GearListener listener) {
        this.plugin = plugin;
        this.store = store;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            if (!sender.hasPermission("gearsense.admin")) return denied(sender);
            plugin.reloadLocalConfiguration();
            sender.sendMessage(plugin.message("reloaded"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Player-only command. Console may use /gearsense reload.");
            return true;
        }
        if (!player.hasPermission("gearsense.use")) return denied(sender);

        PlayerSettings settings = store.get(player);
        switch (sub) {
            case "on" -> {
                settings = settings.withEnabled(true);
                store.save(player, settings);
                player.sendMessage(plugin.message("enabled"));
            }
            case "off" -> {
                settings = settings.withEnabled(false);
                store.save(player, settings);
                listener.cancelRestore(player);
                player.sendMessage(plugin.message("disabled"));
            }
            case "refill" -> {
                if (!player.hasPermission("gearsense.refill")) return denied(sender);
                settings = settings.withRefill(!settings.refill());
                store.save(player, settings);
                player.sendMessage(plugin.message(settings.refill() ? "refill-enabled" : "refill-disabled"));
            }
            case "armor" -> {
                if (!player.hasPermission("gearsense.armor")) return denied(sender);
                settings = settings.withArmorReplacement(!settings.armorReplacement());
                store.save(player, settings);
                player.sendMessage(plugin.message(settings.armorReplacement() ? "armor-enabled" : "armor-disabled"));
            }
            case "restore" -> {
                settings = settings.withRestoreSlot(!settings.restoreSlot());
                store.save(player, settings);
                player.sendMessage(plugin.message(settings.restoreSlot() ? "restore-enabled" : "restore-disabled"));
            }
            case "lock" -> {
                settings = settings.withLocked(!settings.locked());
                store.save(player, settings);
                listener.cancelRestore(player);
                player.sendMessage(plugin.message(settings.locked() ? "locked" : "unlocked"));
            }
            case "prefer" -> {
                if (args.length < 2 || !PREFERENCES.contains(args[1].toLowerCase(Locale.ROOT))) {
                    player.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " prefer <none|speed|fortune|silk-touch|durability>");
                    return true;
                }
                settings = settings.withPreference(Preference.parse(args[1]));
                store.save(player, settings);
                player.sendMessage(plugin.message("preference").replace("%preference%", settings.preference().name()));
            }
            case "status" -> sendStatus(player, settings);
            default -> player.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " [" + String.join("|", ROOT) + "]");
        }
        return true;
    }

    private void sendStatus(Player player, PlayerSettings settings) {
        player.sendMessage(ChatColor.AQUA + "GearSense " + ChatColor.GRAY + "status");
        player.sendMessage(ChatColor.GRAY + "Tool selection: " + flag(settings.enabled()));
        player.sendMessage(ChatColor.GRAY + "Refill: " + flag(settings.refill()));
        player.sendMessage(ChatColor.GRAY + "Armor replacement: " + flag(settings.armorReplacement()));
        player.sendMessage(ChatColor.GRAY + "Restore slot: " + flag(settings.restoreSlot()));
        player.sendMessage(ChatColor.GRAY + "Slot lock: " + flag(settings.locked()));
        player.sendMessage(ChatColor.GRAY + "Preference: " + ChatColor.WHITE + settings.preference().name());
    }

    private String flag(boolean enabled) {
        return (enabled ? ChatColor.GREEN : ChatColor.RED) + (enabled ? "ON" : "OFF");
    }

    private boolean denied(CommandSender sender) {
        sender.sendMessage(plugin.message("no-permission"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(ROOT, args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("prefer")) return filter(PREFERENCES, args[1]);
        return Collections.emptyList();
    }

    private static List<String> filter(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) if (value.startsWith(normalized)) result.add(value);
        return result;
    }
}
