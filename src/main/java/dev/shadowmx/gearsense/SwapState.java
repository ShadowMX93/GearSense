package dev.shadowmx.gearsense;

import org.bukkit.scheduler.BukkitTask;
import org.bukkit.inventory.ItemStack;

record SwapState(
        int originalSlot,
        int selectedToolSlot,
        int inventorySourceSlot,
        ItemStack movedTool,
        ItemStack displacedItem,
        BukkitTask restoreTask
) {
    boolean movedFromInventory() {
        return inventorySourceSlot >= 9;
    }
}
