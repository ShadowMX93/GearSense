package dev.shadowmx.gearsense;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolSelectorTest {
    private final ToolSelector selector = new ToolSelector();

    @Test
    void doesNotSelectToolWhenHeldSlotIsNull() {
        assertTrue(selector.select(playerHolding(null), null, null).isEmpty());
    }

    @Test
    void doesNotSelectToolWhenHeldSlotContainsAir() {
        assertTrue(selector.select(playerHolding(new ItemStack(Material.AIR)), null, null).isEmpty());
    }

    @Test
    void doesNotSelectToolWhenPlayerIsHoldingCombatItem() {
        Material[] combatItems = {
                Material.IRON_SWORD,
                Material.BOW,
                Material.CROSSBOW,
                Material.TRIDENT
        };

        for (Material combatItem : combatItems) {
            assertTrue(
                    selector.select(playerHolding(new ItemStack(combatItem)), null, null).isEmpty(),
                    () -> combatItem + " should not trigger tool selection"
            );
        }
    }

    @Test
    void debugExplainsCombatItemSkip() {
        List<String> messages = new ArrayList<>();

        selector.select(playerHolding(new ItemStack(Material.IRON_SWORD)), null, null, messages::add);

        assertTrue(messages.stream().anyMatch(message -> message.contains("reason=combat-item")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("held-item=IRON_SWORD")));
    }

    @Test
    void doesNotSelectToolWhenPlayerIsHoldingPlaceableBlock() {
        List<String> messages = new ArrayList<>();

        assertTrue(selector.select(
                playerHolding(new ItemStack(Material.STONE)), null, null, messages::add
        ).isEmpty());

        assertTrue(messages.stream().anyMatch(message -> message.contains("reason=placeable-block")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("held-item=STONE")));
    }

    @Test
    void doesNotSelectToolWhenPlayerIsHoldingAnyOtherNonToolItem() {
        List<String> messages = new ArrayList<>();

        assertTrue(selector.select(
                playerHolding(new ItemStack(Material.STICK)), null, null, messages::add
        ).isEmpty());

        assertTrue(messages.stream().anyMatch(message -> message.contains("reason=non-tool-item")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("held-item=STICK")));
    }

    private Player playerHolding(ItemStack heldItem) {
        PlayerInventory inventory = proxy(PlayerInventory.class, (methodName, returnType) -> switch (methodName) {
            case "getHeldItemSlot" -> 0;
            case "getItem" -> heldItem;
            default -> defaultValue(returnType);
        });
        return proxy(Player.class, (methodName, returnType) ->
                methodName.equals("getInventory") ? inventory : defaultValue(returnType));
    }

    private <T> T proxy(Class<T> type, Stub stub) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (ignored, method, ignoredArgs) -> stub.invoke(method.getName(), method.getReturnType())
        ));
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        throw new IllegalArgumentException("Unknown primitive: " + type);
    }

    @FunctionalInterface
    private interface Stub {
        Object invoke(String methodName, Class<?> returnType);
    }
}
