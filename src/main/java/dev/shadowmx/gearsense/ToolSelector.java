package dev.shadowmx.gearsense;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
import java.util.OptionalInt;
import java.util.function.Consumer;

public final class ToolSelector {
    public OptionalInt select(Player player, Block block, PlayerSettings settings) {
        return select(player, block, settings, ignored -> { });
    }

    OptionalInt select(Player player, Block block, PlayerSettings settings, Consumer<String> debug) {
        PlayerInventory inventory = player.getInventory();
        int heldSlot = inventory.getHeldItemSlot();
        ItemStack heldItem = inventory.getItem(heldSlot);

        // Only switch between mining tools. Every other held item is an
        // intentional player choice and must not be replaced by a block-damage
        // event.
        if (heldItem == null || heldItem.getType().isAir()) {
            debug.accept("selection skipped reason=empty-hand held-slot=" + heldSlot);
            return OptionalInt.empty();
        }
        if (isCombatItem(heldItem.getType())) {
            debug.accept("selection skipped reason=combat-item held-slot=" + heldSlot
                    + " held-item=" + heldItem.getType());
            return OptionalInt.empty();
        }
        if (heldItem.getType().isBlock()) {
            debug.accept("selection skipped reason=placeable-block held-slot=" + heldSlot
                    + " held-item=" + heldItem.getType());
            return OptionalInt.empty();
        }
        if (!isTool(heldItem.getType())) {
            debug.accept("selection skipped reason=non-tool-item held-slot=" + heldSlot
                    + " held-item=" + heldItem.getType());
            return OptionalInt.empty();
        }

        // Once GearSense has a suitable tool in the player's hand, keep using
        // it. Re-ranking duplicate tools by durability on every block caused
        // visible slot bouncing and prevented a worn tool from being finished.
        if (block.isPreferredTool(heldItem)) {
            debug.accept("selection kept held tool slot=" + heldSlot + " item=" + heldItem.getType()
                    + " reason=already-preferred");
            return OptionalInt.of(heldSlot);
        }

        int endExclusive = settings.searchEntireInventory() ? 36 : 9;
        ToolScore best = null;
        ToolScore bestUnsafe = null;

        for (int slot = 0; slot < endExclusive; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir() || !isTool(item.getType())) {
                continue;
            }

            ToolScore score = score(slot, item, block, settings.preference());
            boolean durabilitySafe = isDurabilitySafe(item, settings.durabilityReserve());
            debug.accept("candidate slot=" + slot + " item=" + item.getType()
                    + " durability-safe=" + durabilitySafe + " score=" + score);
            if (bestUnsafe == null || score.compareTo(bestUnsafe) > 0) {
                bestUnsafe = score;
            }
            if (durabilitySafe && (best == null || score.compareTo(best) > 0)) {
                best = score;
            }
        }

        // A nearly-broken tool is used only if it is the only viable choice.
        ToolScore selected = best != null ? best : bestUnsafe;
        if (selected == null || !selected.preferredTool()) {
            debug.accept("selection skipped reason=no-preferred-tool");
            return OptionalInt.empty();
        }
        debug.accept("selection chose slot=" + selected.slot() + " score=" + selected);
        return OptionalInt.of(selected.slot());
    }

    ToolScore score(int slot, ItemStack item, Block block, Preference preference) {
        boolean preferred = block.isPreferredTool(item);
        int efficiency = item.getEnchantmentLevel(Enchantment.DIG_SPEED);
        int fortune = item.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
        int silk = item.getEnchantmentLevel(Enchantment.SILK_TOUCH);
        int unbreaking = item.getEnchantmentLevel(Enchantment.DURABILITY);
        int remaining = durabilityRemaining(item);

        int preferenceScore = switch (preference) {
            case FORTUNE -> fortune * 100;
            case SILK_TOUCH -> silk * 100;
            case DURABILITY -> remaining;
            case SPEED -> toolTier(item.getType()) * 30 + efficiency * 15;
            case NONE -> 0;
        };
        int speedScore = toolTier(item.getType()) * 30 + efficiency * 15 + toolAffinity(item.getType(), block.getType());
        int enchantmentScore = efficiency * 4 + unbreaking + fortune + silk;
        return new ToolScore(slot, preferred, preferenceScore, speedScore, enchantmentScore, remaining);
    }

    static boolean isTool(Material material) {
        String name = material.name();
        return name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE") || material == Material.SHEARS;
    }

    static boolean isCombatItem(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || name.equals("BOW") || name.equals("CROSSBOW")
                || name.equals("TRIDENT") || name.equals("MACE");
    }

    static boolean isDurabilitySafe(ItemStack item, int reserve) {
        if (item.getType().getMaxDurability() <= 0) return true;
        return durabilityRemaining(item) > reserve;
    }

    static int durabilityRemaining(ItemStack item) {
        int max = item.getType().getMaxDurability();
        ItemMeta meta = item.getItemMeta();
        if (max <= 0 || !(meta instanceof Damageable damageable)) return Integer.MAX_VALUE;
        return Math.max(0, max - damageable.getDamage());
    }

    private static int toolTier(Material material) {
        String name = material.name();
        if (name.startsWith("NETHERITE_")) return 6;
        if (name.startsWith("DIAMOND_")) return 5;
        if (name.startsWith("GOLDEN_")) return 4;
        if (name.startsWith("IRON_")) return 3;
        if (name.startsWith("STONE_")) return 2;
        if (name.startsWith("WOODEN_")) return 1;
        return material == Material.SHEARS ? 5 : 0;
    }

    private static int toolAffinity(Material tool, Material block) {
        String toolName = tool.name();
        String blockName = block.name().toUpperCase(Locale.ROOT);
        if (tool == Material.SHEARS && (blockName.contains("LEAVES") || blockName.contains("WOOL")
                || blockName.contains("VINE") || blockName.contains("WEB"))) return 500;
        if (toolName.endsWith("_AXE") && (blockName.contains("LOG") || blockName.contains("WOOD")
                || blockName.contains("PLANK") || blockName.contains("STEM") || blockName.contains("HYPHAE"))) return 80;
        if (toolName.endsWith("_SHOVEL") && (blockName.contains("DIRT") || blockName.contains("SAND")
                || blockName.contains("GRAVEL") || blockName.contains("CLAY") || blockName.contains("SNOW"))) return 80;
        if (toolName.endsWith("_HOE") && (blockName.contains("LEAVES") || blockName.contains("WART_BLOCK")
                || blockName.contains("SCULK") || blockName.contains("SPONGE"))) return 80;
        if (toolName.endsWith("_PICKAXE")) return 20;
        return 0;
    }
}
