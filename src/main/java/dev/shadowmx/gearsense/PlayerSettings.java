package dev.shadowmx.gearsense;

public record PlayerSettings(
        boolean enabled,
        boolean refill,
        boolean armorReplacement,
        boolean restoreSlot,
        boolean locked,
        boolean searchEntireInventory,
        int durabilityReserve,
        Preference preference
) {
    public PlayerSettings withEnabled(boolean value) {
        return new PlayerSettings(value, refill, armorReplacement, restoreSlot, locked, searchEntireInventory, durabilityReserve, preference);
    }

    public PlayerSettings withRefill(boolean value) {
        return new PlayerSettings(enabled, value, armorReplacement, restoreSlot, locked, searchEntireInventory, durabilityReserve, preference);
    }

    public PlayerSettings withArmorReplacement(boolean value) {
        return new PlayerSettings(enabled, refill, value, restoreSlot, locked, searchEntireInventory, durabilityReserve, preference);
    }

    public PlayerSettings withRestoreSlot(boolean value) {
        return new PlayerSettings(enabled, refill, armorReplacement, value, locked, searchEntireInventory, durabilityReserve, preference);
    }

    public PlayerSettings withLocked(boolean value) {
        return new PlayerSettings(enabled, refill, armorReplacement, restoreSlot, value, searchEntireInventory, durabilityReserve, preference);
    }

    public PlayerSettings withPreference(Preference value) {
        return new PlayerSettings(enabled, refill, armorReplacement, restoreSlot, locked, searchEntireInventory, durabilityReserve, value);
    }
}
