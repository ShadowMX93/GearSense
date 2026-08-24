package dev.shadowmx.gearsense;

import java.util.Locale;

public enum Preference {
    NONE,
    SPEED,
    FORTUNE,
    SILK_TOUCH,
    DURABILITY;

    public static Preference parse(String value) {
        if (value == null) {
            return NONE;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (normalized.equals("SILK")) {
            return SILK_TOUCH;
        }
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}
