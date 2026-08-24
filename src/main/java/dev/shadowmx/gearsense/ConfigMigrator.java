package dev.shadowmx.gearsense;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.logging.Level;

final class ConfigMigrator {
    static final int CURRENT_VERSION = 1;

    private ConfigMigrator() {
    }

    static void migrate(JavaPlugin plugin) {
        Path configPath = plugin.getDataFolder().toPath().resolve("config.yml");
        if (!Files.isRegularFile(configPath)) {
            return;
        }

        try (InputStream defaultsStream = Objects.requireNonNull(
                plugin.getResource("config.yml"), "Bundled config.yml is missing")) {
            String currentYaml = Files.readString(configPath, StandardCharsets.UTF_8);
            YamlConfiguration current = load(currentYaml);
            int currentVersion = current.getInt("config-version", 0);
            if (currentVersion >= CURRENT_VERSION) {
                return;
            }

            String defaultsYaml = new String(defaultsStream.readAllBytes(), StandardCharsets.UTF_8);
            String migratedYaml = merge(defaultsYaml, currentYaml);
            Path backupPath = nextBackupPath(configPath, currentVersion);
            Files.copy(configPath, backupPath);
            replaceAtomically(configPath, migratedYaml);

            plugin.getLogger().info("Updated config.yml from format " + currentVersion
                    + " to " + CURRENT_VERSION + "; backup saved as " + backupPath.getFileName() + '.');
        } catch (IOException | InvalidConfigurationException | RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not update config.yml. The original file was left unchanged.", exception);
        }
    }

    static String merge(String defaultsYaml, String currentYaml) throws InvalidConfigurationException {
        YamlConfiguration defaults = load(defaultsYaml);
        YamlConfiguration current = load(currentYaml);

        for (String path : current.getKeys(true)) {
            if (!current.isConfigurationSection(path) && !path.equals("config-version")) {
                defaults.set(path, current.get(path));
            }
        }
        defaults.set("config-version", CURRENT_VERSION);
        return defaults.saveToString();
    }

    private static YamlConfiguration load(String yaml) throws InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.options().parseComments(true);
        configuration.loadFromString(yaml);
        return configuration;
    }

    private static Path nextBackupPath(Path configPath, int oldVersion) {
        Path directory = configPath.getParent();
        String baseName = "config.yml.v" + oldVersion + ".backup";
        Path candidate = directory.resolve(baseName);
        int suffix = 2;
        while (Files.exists(candidate)) {
            candidate = directory.resolve(baseName + '.' + suffix++);
        }
        return candidate;
    }

    private static void replaceAtomically(Path configPath, String contents) throws IOException {
        Path temporary = Files.createTempFile(configPath.getParent(), "config.yml.", ".tmp");
        try {
            Files.writeString(temporary, contents, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, configPath, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, configPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
