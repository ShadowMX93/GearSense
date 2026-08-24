package dev.shadowmx.gearsense;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigratorTest {
    private static final String DEFAULTS = """
            # Current documented configuration.
            config-version: 1
            defaults:
              # Enable the feature for new players.
              enabled: false
              new-option: true
            updater:
              auto-download: false
            """;

    @Test
    void addsNewOptionsWhileKeepingExistingChoicesAndUnknownKeys() throws Exception {
        String oldConfig = """
                defaults:
                  enabled: true
                updater:
                  auto-download: true
                custom-extension:
                  special-mode: enabled
                """;

        String mergedText = ConfigMigrator.merge(DEFAULTS, oldConfig);
        YamlConfiguration merged = new YamlConfiguration();
        merged.loadFromString(mergedText);

        assertEquals(1, merged.getInt("config-version"));
        assertTrue(merged.getBoolean("defaults.enabled"));
        assertTrue(merged.getBoolean("defaults.new-option"));
        assertTrue(merged.getBoolean("updater.auto-download"));
        assertEquals("enabled", merged.getString("custom-extension.special-mode"));
        assertTrue(mergedText.contains("Enable the feature for new players."));
    }
}
