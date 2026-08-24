package dev.shadowmx.gearsense;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateService implements Listener {
    private static final String REPOSITORY = "ShadowMX93/GearSense";
    private static final String LATEST_RELEASE_API = "https://api.github.com/repos/" + REPOSITORY + "/releases/latest";
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 20_000;
    private static final int MAX_RESPONSE_BYTES = 1_048_576;
    private static final long MAX_JAR_BYTES = 100L * 1024L * 1024L;
    private static final Pattern TAG_PATTERN = Pattern.compile("\\\"tag_name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern RELEASE_URL_PATTERN = Pattern.compile("\\\"html_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern JAR_URL_PATTERN = Pattern.compile(
            "\\\"browser_download_url\\\"\\s*:\\s*\\\"([^\\\"]+\\.jar)\\\"",
            Pattern.CASE_INSENSITIVE
    );

    private final GearSensePlugin plugin;
    private final AtomicBoolean checking = new AtomicBoolean();
    private final String currentVersion;
    private BukkitTask scheduledTask;
    private volatile ReleaseInfo latestRelease;
    private volatile String downloadedVersion;
    private volatile String lastError;
    private volatile long lastCheckTime;
    private volatile boolean autoDownload;
    private volatile boolean notifyAdmins;

    public UpdateService(GearSensePlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public void start() {
        restart();
    }

    public void restart() {
        stop();
        autoDownload = plugin.getConfig().getBoolean("updater.auto-download", false);
        notifyAdmins = plugin.getConfig().getBoolean("updater.notify-admins", true);
        if (!plugin.getConfig().getBoolean("updater.enabled", true)) return;

        long hours = Math.max(1L, plugin.getConfig().getLong("updater.check-interval-hours", 12L));
        long periodTicks = Math.multiplyExact(Math.min(hours, 24L * 365L), 72_000L);
        long initialDelay = plugin.getConfig().getBoolean("updater.check-on-startup", true) ? 100L : periodTicks;
        scheduledTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                () -> performCheck(null, false),
                initialDelay,
                periodTicks
        );
    }

    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }

    public void checkNow(CommandSender requester, boolean downloadRequested) {
        if (!checking.compareAndSet(false, true)) {
            requester.sendMessage(plugin.message("update-already-checking"));
            return;
        }
        requester.sendMessage(plugin.message("update-checking"));
        plugin.getServer().getScheduler().runTaskAsynchronously(
                plugin,
                () -> performCheckAlreadyLocked(requester, downloadRequested)
        );
    }

    public void sendStatus(CommandSender sender) {
        ReleaseInfo release = latestRelease;
        if (lastCheckTime == 0L) {
            sender.sendMessage(plugin.message("update-not-checked"));
        } else if (lastError != null) {
            sender.sendMessage(plugin.message("update-check-failed").replace("%error%", lastError));
        } else if (release != null && isNewer(release.version())) {
            String key = release.version().equals(downloadedVersion) ? "update-ready" : "update-available";
            sender.sendMessage(formatReleaseMessage(key, release));
        } else {
            sender.sendMessage(plugin.message("update-current").replace("%version%", currentVersion));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!notifyAdmins) return;
        Player player = event.getPlayer();
        ReleaseInfo release = latestRelease;
        if (player.hasPermission("gearsense.update") && release != null && isNewer(release.version())) {
            plugin.getServer().getScheduler().runTaskLater(
                    plugin,
                    () -> player.sendMessage(formatReleaseMessage(
                            release.version().equals(downloadedVersion) ? "update-ready" : "update-available",
                            release
                    )),
                    40L
            );
        }
    }

    private void performCheck(CommandSender requester, boolean downloadRequested) {
        if (!checking.compareAndSet(false, true)) return;
        performCheckAlreadyLocked(requester, downloadRequested);
    }

    private void performCheckAlreadyLocked(CommandSender requester, boolean downloadRequested) {
        try {
            ReleaseInfo release = fetchLatestRelease();
            latestRelease = release;
            lastCheckTime = System.currentTimeMillis();
            lastError = null;

            boolean newer = isNewer(release.version());
            boolean shouldDownload = newer && (downloadRequested || autoDownload);
            if (shouldDownload) {
                downloadRelease(release);
                downloadedVersion = release.version();
            }
            completeOnMainThread(requester, release, newer, shouldDownload, null);
        } catch (Exception exception) {
            lastCheckTime = System.currentTimeMillis();
            lastError = conciseError(exception);
            completeOnMainThread(requester, null, false, false, exception);
        } finally {
            checking.set(false);
        }
    }

    private void completeOnMainThread(CommandSender requester, ReleaseInfo release, boolean newer,
                                      boolean downloaded, Exception failure) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (failure != null) {
                plugin.getLogger().log(Level.WARNING, "Unable to check for GearSense updates: " + conciseError(failure));
                if (requester != null) {
                    requester.sendMessage(plugin.message("update-check-failed").replace("%error%", conciseError(failure)));
                }
                return;
            }
            if (!newer) {
                if (requester != null) {
                    requester.sendMessage(plugin.message("update-current").replace("%version%", currentVersion));
                }
                return;
            }
            if (downloaded) {
                plugin.getLogger().info("Downloaded GearSense " + release.version()
                        + " to the server update folder. Restart to install it.");
                if (requester != null) requester.sendMessage(formatReleaseMessage("update-ready", release));
            } else {
                plugin.getLogger().info("GearSense " + release.version() + " is available: " + release.releaseUrl());
                if (requester != null) requester.sendMessage(formatReleaseMessage("update-available", release));
            }
        });
    }

    private ReleaseInfo fetchLatestRelease() throws IOException {
        HttpURLConnection connection = openConnection(LATEST_RELEASE_API);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("GitHub returned HTTP " + responseCode);
        }
        String json;
        try (InputStream input = connection.getInputStream()) {
            json = new String(readLimited(input, MAX_RESPONSE_BYTES), StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }

        String tag = requiredMatch(TAG_PATTERN, json, "release tag");
        String releaseUrl = requiredMatch(RELEASE_URL_PATTERN, json, "release URL");
        Matcher jarMatcher = JAR_URL_PATTERN.matcher(json);
        String jarUrl = jarMatcher.find() ? unescapeJson(jarMatcher.group(1)) : null;
        return new ReleaseInfo(normalizeVersion(tag), unescapeJson(releaseUrl), jarUrl);
    }

    private void downloadRelease(ReleaseInfo release) throws IOException {
        if (!release.hasJar()) {
            throw new IOException("The latest release has no JAR asset");
        }
        URI uri = URI.create(release.jarUrl());
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !"github.com".equalsIgnoreCase(uri.getHost())) {
            throw new IOException("Refusing an unexpected release download URL");
        }

        File pluginsDirectory = plugin.getDataFolder().getParentFile();
        Path updateDirectory = new File(pluginsDirectory, "update").toPath();
        Files.createDirectories(updateDirectory);
        Path destination = updateDirectory.resolve("GearSense-" + release.version() + ".jar");
        Path temporary = Files.createTempFile(updateDirectory, "gearsense-update-", ".tmp");

        try {
            HttpURLConnection connection = openConnection(release.jarUrl());
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Release download returned HTTP " + responseCode);
            }
            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 OutputStream output = new BufferedOutputStream(Files.newOutputStream(temporary))) {
                copyLimited(input, output, MAX_JAR_BYTES);
            } finally {
                connection.disconnect();
            }
            validateJar(temporary, release.version());
            moveAtomically(temporary, destination);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static HttpURLConnection openConnection(String url) throws IOException {
        URL parsed = URI.create(url).toURL();
        HttpURLConnection connection = (HttpURLConnection) parsed.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "GearSense-Updater");
        return connection;
    }

    private static byte[] readLimited(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > limit) throw new IOException("GitHub response was unexpectedly large");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static void copyLimited(InputStream input, OutputStream output, long limit) throws IOException {
        byte[] buffer = new byte[8192];
        long total = 0L;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > limit) throw new IOException("Release JAR exceeds 100 MiB");
            output.write(buffer, 0, read);
        }
    }

    private static void validateJar(Path path, String expectedVersion) throws IOException {
        try (JarFile jar = new JarFile(path.toFile())) {
            if (jar.getEntry("plugin.yml") == null) throw new IOException("Downloaded file is not a GearSense plugin JAR");
            String descriptor;
            try (InputStream input = jar.getInputStream(jar.getEntry("plugin.yml"))) {
                descriptor = new String(readLimited(input, 65_536), StandardCharsets.UTF_8);
            }
            if (!Pattern.compile("(?m)^name:\\s*GearSense\\s*$").matcher(descriptor).find()) {
                throw new IOException("Downloaded JAR has the wrong plugin name");
            }
            Pattern versionPattern = Pattern.compile("(?m)^version:\\s*['\\\"]?" + Pattern.quote(expectedVersion) + "['\\\"]?\\s*$");
            if (!versionPattern.matcher(descriptor).find()) {
                throw new IOException("Downloaded JAR version does not match release " + expectedVersion);
            }
        }
    }

    private static void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean isNewer(String candidate) {
        return VersionNumber.parse(candidate).compareTo(VersionNumber.parse(currentVersion)) > 0;
    }

    private String formatReleaseMessage(String key, ReleaseInfo release) {
        return plugin.message(key)
                .replace("%current%", currentVersion)
                .replace("%latest%", release.version())
                .replace("%url%", release.releaseUrl());
    }

    private static String requiredMatch(Pattern pattern, String input, String description) throws IOException {
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) throw new IOException("GitHub response did not contain a " + description);
        return unescapeJson(matcher.group(1));
    }

    private static String normalizeVersion(String value) {
        String trimmed = value.trim();
        return trimmed.toLowerCase(Locale.ROOT).startsWith("v") ? trimmed.substring(1) : trimmed;
    }

    private static String unescapeJson(String value) {
        return value.replace("\\/", "/").replace("\\\\", "\\").replace("\\\"", "\"");
    }

    private static String conciseError(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
