package dev.shadowmx.gearsense;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

public final class GearListener implements Listener {
    private final GearSensePlugin plugin;
    private final SettingsStore settingsStore;
    private final ToolSelector selector;
    private final Map<UUID, SwapState> swaps = new HashMap<>();

    public GearListener(GearSensePlugin plugin, SettingsStore settingsStore, ToolSelector selector) {
        this.plugin = plugin;
        this.settingsStore = settingsStore;
        this.selector = selector;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (!canUse(player)) return;
        PlayerSettings settings = settingsStore.get(player);
        if (!settings.enabled() || settings.locked()) return;
        if (plugin.isShiftBypass() && player.isSneaking()) return;

        Block block = event.getBlock();
        Set<Material> ignored = plugin.getIgnoredBlocks();
        if (ignored.contains(block.getType())) return;

        SwapState previous = swaps.get(player.getUniqueId());
        if (previous != null && previous.movedFromInventory()) {
            restore(player);
        }

        OptionalInt selected = selector.select(player, block, settings);
        if (selected.isEmpty()) return;
        int selectedSlot = selected.getAsInt();
        PlayerInventory inventory = player.getInventory();
        int heldSlot = inventory.getHeldItemSlot();

        if (selectedSlot > 8) {
            ItemStack selectedItem = inventory.getItem(selectedSlot);
            ItemStack heldItem = inventory.getItem(heldSlot);
            inventory.setItem(heldSlot, selectedItem);
            inventory.setItem(selectedSlot, heldItem);
            swaps.put(player.getUniqueId(), new SwapState(
                    heldSlot,
                    heldSlot,
                    selectedSlot,
                    cloneOrNull(selectedItem),
                    cloneOrNull(heldItem),
                    null
            ));
            return;
        }
        if (selectedSlot == heldSlot) return;

        SwapState existing = swaps.remove(player.getUniqueId());
        int originalSlot = existing == null ? heldSlot : existing.originalSlot();
        if (existing != null && existing.restoreTask() != null) existing.restoreTask().cancel();
        inventory.setHeldItemSlot(selectedSlot);
        if (plugin.isStickyTool()) return;
        swaps.put(player.getUniqueId(), new SwapState(originalSlot, selectedSlot, -1, null, null, null));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerSettings settings = settingsStore.get(player);
        if (plugin.isStickyTool() || !settings.restoreSlot()) return;
        scheduleRestore(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        scheduleRefill(event.getPlayer(), event.getItemInHand());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        scheduleRefill(event.getPlayer(), event.getItem());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(PlayerItemBreakEvent event) {
        ItemStack brokenItem = event.getBrokenItem();
        if (armorSlot(brokenItem.getType()) != null) {
            scheduleArmorReplacement(event.getPlayer(), brokenItem);
        } else {
            scheduleToolReplacement(event.getPlayer(), brokenItem);
            scheduleRefill(event.getPlayer(), brokenItem);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && !item.getType().isAir() && !ToolSelector.isTool(item.getType())) {
            scheduleRefill(event.getPlayer(), item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) clearRestoreIfPlayerChangedSlot(event.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        cancelRestore(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldSlotChange(PlayerItemHeldEvent event) {
        SwapState state = swaps.get(event.getPlayer().getUniqueId());
        if (state != null && state.movedFromInventory()) {
            restore(event.getPlayer());
        } else {
            cancelRestore(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelRestore(event.getPlayer());
    }

    public void cancelRestore(Player player) {
        SwapState state = swaps.remove(player.getUniqueId());
        if (state != null && state.restoreTask() != null) state.restoreTask().cancel();
    }

    private boolean canUse(Player player) {
        return player.hasPermission("gearsense.use")
                && player.getGameMode() != GameMode.CREATIVE
                && player.getGameMode() != GameMode.SPECTATOR;
    }

    private void scheduleRestore(Player player) {
        SwapState state = swaps.get(player.getUniqueId());
        if (state == null) return;
        if (state.restoreTask() != null) state.restoreTask().cancel();
        int delay = plugin.getRestoreDelayTicks();
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> restore(player), Math.max(0, delay));
        swaps.put(player.getUniqueId(), new SwapState(
                state.originalSlot(), state.selectedToolSlot(), state.inventorySourceSlot(),
                state.movedTool(), state.displacedItem(), task
        ));
    }

    private void restore(Player player) {
        SwapState state = swaps.remove(player.getUniqueId());
        if (state == null || !player.isOnline()) return;
        if (state.movedFromInventory()) {
            PlayerInventory inventory = player.getInventory();
            ItemStack held = inventory.getItem(state.originalSlot());
            ItemStack source = inventory.getItem(state.inventorySourceSlot());
            if (sameIdentityIgnoringDamage(held, state.movedTool())
                    && sameIdentityIgnoringDamage(source, state.displacedItem())) {
                inventory.setItem(state.originalSlot(), source);
                inventory.setItem(state.inventorySourceSlot(), held);
            }
        } else if (player.getInventory().getHeldItemSlot() == state.selectedToolSlot()) {
            player.getInventory().setHeldItemSlot(state.originalSlot());
        }
    }

    private void clearRestoreIfPlayerChangedSlot(Player player) {
        SwapState state = swaps.get(player.getUniqueId());
        if (state != null && player.getInventory().getHeldItemSlot() != state.selectedToolSlot()) {
            cancelRestore(player);
        }
    }

    private void scheduleRefill(Player player, ItemStack template) {
        ItemStack snapshot = cloneOrNull(template);
        if (snapshot == null) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> refill(player, snapshot));
    }

    private void scheduleToolReplacement(Player player, ItemStack brokenItem) {
        ItemStack snapshot = cloneOrNull(brokenItem);
        if (snapshot == null || !ToolSelector.isTool(snapshot.getType())) return;
        PlayerSettings settings = settingsStore.get(player);
        if (!settings.enabled() || !player.hasPermission("gearsense.use")) return;
        cancelRestore(player);
        plugin.getServer().getScheduler().runTask(plugin, () -> replaceBrokenTool(player, snapshot));
    }

    private void scheduleArmorReplacement(Player player, ItemStack brokenItem) {
        ItemStack snapshot = cloneOrNull(brokenItem);
        EquipmentSlot target = snapshot == null ? null : armorSlot(snapshot.getType());
        if (target == null || !player.hasPermission("gearsense.armor")) return;
        PlayerSettings settings = settingsStore.get(player);
        if (!settings.armorReplacement()) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> replaceBrokenArmor(player, snapshot, target));
    }

    private void replaceBrokenArmor(Player player, ItemStack brokenItem, EquipmentSlot target) {
        if (!player.isOnline()) return;
        PlayerInventory inventory = player.getInventory();
        ItemStack equipped = getArmor(inventory, target);
        if (equipped != null && !equipped.getType().isAir()) return;

        int materialFallback = -1;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (candidate == null || candidate.getType().isAir() || armorSlot(candidate.getType()) != target) continue;
            if (sameIdentityIgnoringDamage(candidate, brokenItem)) {
                equipFromInventory(inventory, target, slot, candidate);
                return;
            }
            if (materialFallback < 0 && candidate.getType() == brokenItem.getType()) {
                materialFallback = slot;
            }
        }
        if (materialFallback >= 0) {
            equipFromInventory(inventory, target, materialFallback, inventory.getItem(materialFallback));
        }
    }

    private void replaceBrokenTool(Player player, ItemStack brokenItem) {
        if (!player.isOnline()) return;
        PlayerInventory inventory = player.getInventory();
        int held = inventory.getHeldItemSlot();
        ItemStack current = inventory.getItem(held);
        if (current != null && !current.getType().isAir()) return;

        PlayerSettings settings = settingsStore.get(player);
        int endExclusive = settings.searchEntireInventory() ? 36 : 9;
        int fallback = -1;
        for (int slot = 0; slot < endExclusive; slot++) {
            if (slot == held) continue;
            ItemStack candidate = inventory.getItem(slot);
            if (candidate == null || candidate.getType().isAir()) continue;
            if (sameIdentityIgnoringDamage(candidate, brokenItem)) {
                moveToHeldSlot(inventory, held, slot, candidate);
                return;
            }
            if (fallback < 0 && candidate.getType() == brokenItem.getType()) {
                fallback = slot;
            }
        }
        if (fallback >= 0) {
            moveToHeldSlot(inventory, held, fallback, inventory.getItem(fallback));
        }
    }

    private static void moveToHeldSlot(PlayerInventory inventory, int held, int source, ItemStack item) {
        inventory.setItem(held, item);
        inventory.setItem(source, null);
    }

    private static EquipmentSlot armorSlot(Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET") || material == Material.TURTLE_HELMET) return EquipmentSlot.HEAD;
        if (name.endsWith("_CHESTPLATE") || material == Material.ELYTRA) return EquipmentSlot.CHEST;
        if (name.endsWith("_LEGGINGS")) return EquipmentSlot.LEGS;
        if (name.endsWith("_BOOTS")) return EquipmentSlot.FEET;
        return null;
    }

    private static ItemStack getArmor(PlayerInventory inventory, EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> inventory.getHelmet();
            case CHEST -> inventory.getChestplate();
            case LEGS -> inventory.getLeggings();
            case FEET -> inventory.getBoots();
            default -> null;
        };
    }

    private static void equipFromInventory(PlayerInventory inventory, EquipmentSlot target, int source, ItemStack item) {
        switch (target) {
            case HEAD -> inventory.setHelmet(item);
            case CHEST -> inventory.setChestplate(item);
            case LEGS -> inventory.setLeggings(item);
            case FEET -> inventory.setBoots(item);
            default -> throw new IllegalArgumentException("Not an armor slot: " + target);
        }
        inventory.setItem(source, null);
    }

    private void refill(Player player, ItemStack template) {
        if (!player.isOnline() || !player.hasPermission("gearsense.refill")) return;
        PlayerSettings settings = settingsStore.get(player);
        if (!settings.refill()) return;

        PlayerInventory inventory = player.getInventory();
        int held = inventory.getHeldItemSlot();
        ItemStack current = inventory.getItem(held);
        if (current != null && !current.getType().isAir() && current.getAmount() > 0) return;

        for (int slot = 9; slot < 36; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (!sameIdentityIgnoringDamage(candidate, template)) continue;
            inventory.setItem(held, candidate);
            inventory.setItem(slot, null);
            return;
        }
    }

    private static ItemStack cloneOrNull(ItemStack item) {
        return item == null || item.getType().isAir() ? null : item.clone();
    }

    private static boolean sameIdentityIgnoringDamage(ItemStack first, ItemStack second) {
        boolean firstEmpty = first == null || first.getType().isAir();
        boolean secondEmpty = second == null || second.getType().isAir();
        if (firstEmpty || secondEmpty) return firstEmpty == secondEmpty;
        ItemStack a = first.clone();
        ItemStack b = second.clone();
        a.setAmount(1);
        b.setAmount(1);
        if (a.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable) {
            damageable.setDamage(0);
            a.setItemMeta(damageable);
        }
        if (b.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable) {
            damageable.setDamage(0);
            b.setItemMeta(damageable);
        }
        return a.isSimilar(b);
    }
}
