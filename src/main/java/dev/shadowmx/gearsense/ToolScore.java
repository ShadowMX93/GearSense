package dev.shadowmx.gearsense;

record ToolScore(
        int slot,
        boolean preferredTool,
        int preferenceScore,
        int speedScore,
        int enchantmentScore,
        int durabilityRemaining
) implements Comparable<ToolScore> {
    @Override
    public int compareTo(ToolScore other) {
        int result = Boolean.compare(preferredTool, other.preferredTool);
        if (result != 0) return result;
        result = Integer.compare(preferenceScore, other.preferenceScore);
        if (result != 0) return result;
        result = Integer.compare(speedScore, other.speedScore);
        if (result != 0) return result;
        result = Integer.compare(enchantmentScore, other.enchantmentScore);
        if (result != 0) return result;
        return Integer.compare(durabilityRemaining, other.durabilityRemaining);
    }
}
