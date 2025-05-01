package com.ninja.ghast.ghastCore;

import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

public class ExtensionManager {
    private final JavaPlugin plugin;
    private final LogManager logger;
    private final File extensionsFolder;
    private final Map<String, ExtensionInfo> extensions;
    private final Map<String, File> pendingExtensions;
    private final Map<String, ExtensionMetadata> extensionMetadata;
    private final boolean continueOnError;
    private final boolean lazyLoad;
    private final boolean recursiveScan;
    private final int maxScanDepth;
    private final boolean asyncScan;
    private final int maxLoaded;
    private final long unloadAfterMinutes;

    public ExtensionManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = new LogManager(plugin.getLogger(), plugin.getConfig());
        this.extensions = new HashMap<>();
        this.pendingExtensions = new ConcurrentHashMap<>();
        this.extensionMetadata = new ConcurrentHashMap<>();
        this.extensionsFolder = new File(plugin.getDataFolder(), "extensions");
        this.continueOnError = plugin.getConfig().getBoolean("extensions.continue-on-error", true);
        this.lazyLoad = plugin.getConfig().getBoolean("extensions.lazy-load", false);
        this.recursiveScan = plugin.getConfig().getBoolean("extensions.recursive-scan", false);
        this.maxScanDepth = plugin.getConfig().getInt("extensions.max-scan-depth", 3);
        this.asyncScan = plugin.getConfig().getBoolean("extensions.async-scan", true);
        this.maxLoaded = plugin.getConfig().getInt("extensions.max-loaded", 15);
        this.unloadAfterMinutes = plugin.getConfig().getLong("extensions.unload-after-minutes", 30);
        setupExtensionsFolder();
    }

    private void setupExtensionsFolder() {
        if (!extensionsFolder.exists()) {
            extensionsFolder.mkdirs();
        }
    }

    public void scanExtensions() {
        pendingExtensions.clear();
        if (recursiveScan) {
            scanExtensionsRecursive();
        } else {
            scanExtensionsFlat();
        }
    }

    private void scanExtensionsFlat() {
        File[] jarFiles = extensionsFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null) return;

        for (File jarFile : jarFiles) {
            if (isValidJar(jarFile)) {
                pendingExtensions.put(jarFile.getName().replace(".jar", ""), jarFile);
                logger.debug("Scanned extension: " + jarFile.getName());
            }
        }
    }

    public void scanExtensionsRecursive() {
        if (asyncScan) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                scanRecursiveSync();
                executor.shutdown();
            });
        } else {
            scanRecursiveSync();
        }
    }

    private void scanRecursiveSync() {
        try (Stream<Path> paths = Files.walk(extensionsFolder.toPath(), maxScanDepth)) {
            paths.filter(path -> path.toString().endsWith(".jar"))
                    .forEach(path -> {
                        File jarFile = path.toFile();
                        if (isValidJar(jarFile)) {
                            pendingExtensions.put(jarFile.getName().replace(".jar", ""), jarFile);
                            logger.debug("Scanned extension: " + jarFile.getName());
                        }
                    });
        } catch (IOException e) {
            logger.severe("Error during recursive scan: " + e.getMessage());
        }
    }

    private boolean isValidJar(File jarFile) {
        try (ZipFile zip = new ZipFile(jarFile)) {
            // Basic validation: check if it's a valid ZIP (JAR) file
            if (Bukkit.getMinecraftVersion().startsWith("1.21") && jarFile.getName().contains("1_20")) {
                logger.warning("Skipping 1.20-specific extension on 1.21: " + jarFile.getName());
                return false;
            }
            return true;
        } catch (IOException e) {
            logger.warning("Invalid JAR file: " + jarFile.getName());
            return false;
        }
    }

    public void loadExtensions() {
        if (lazyLoad) {
            scanExtensions();
            return;
        }

        for (File jarFile : pendingExtensions.values()) {
            try {
                loadExtension(jarFile);
            } catch (Exception e) {
                if (continueOnError) {
                    logger.severe("Failed to load extension " + jarFile.getName() + ": " + e.getMessage());
                } else {
                    logger.severe("Failed to load extension " + jarFile.getName() + ": " + e.getMessage());
                    throw new RuntimeException("Extension loading failed", e);
                }
            }
        }
        pendingExtensions.clear();
        enforceMaxLoaded();
    }

    public boolean loadExtension(@NotNull String extensionName) {
        File jarFile = pendingExtensions.get(extensionName);
        if (jarFile == null) {
            logger.warning("No pending extension found: " + extensionName);
            return false;
        }
        try {
            loadExtension(jarFile);
            pendingExtensions.remove(extensionName);
            enforceMaxLoaded();
            return true;
        } catch (Exception e) {
            if (continueOnError) {
                logger.severe("Failed to load extension " + jarFile.getName() + ": " + e.getMessage());
                return false;
            } else {
                logger.severe("Failed to load extension " + jarFile.getName() + ": " + e.getMessage());
                throw new RuntimeException("Extension loading failed", e);
            }
        }
    }

    public void loadAllPending() {
        for (String extensionName : new HashMap<>(pendingExtensions).keySet()) {
            loadExtension(extensionName);
        }
    }

    private void loadExtension(@NotNull File jarFile) throws InvalidPluginException, InvalidDescriptionException {
        Plugin extension = plugin.getServer().getPluginManager().loadPlugin(jarFile);
        if (extension != null) {
            plugin.getServer().getPluginManager().enablePlugin(extension);
            String extensionName = extension.getName();
            if (!extensions.containsKey(extensionName)) {
                ExtensionInfo info = new ExtensionInfo(
                        extensionName,
                        extension.getDescription().getAuthors().isEmpty() ? "Unknown" : String.join(", ", extension.getDescription().getAuthors()),
                        extension.getDescription().getVersion(),
                        extensionName.toLowerCase()
                );
                extensions.put(extensionName, info);
                extensionMetadata.put(extensionName, new ExtensionMetadata(System.currentTimeMillis(), true));
                logger.info("Loaded extension: " + extensionName);
            }
        }
    }

    public void registerExtension(@NotNull JavaPlugin extension, @NotNull ExtensionInfo info) {
        extensions.put(info.name, info);
        extensionMetadata.computeIfAbsent(info.name, k -> new ExtensionMetadata(System.currentTimeMillis(), true));
        logger.info("Registered extension: " + info.name);
    }

    public void unregisterExtension(@NotNull String extensionName) {
        if (extensions.remove(extensionName) != null) {
            extensionMetadata.remove(extensionName);
            logger.info("Unregistered extension: " + extensionName);
        }
    }

    public void unloadIdleExtensions() {
        long currentTime = System.currentTimeMillis();
        long unloadThreshold = unloadAfterMinutes * 60 * 1000;

        extensionMetadata.entrySet().stream()
                .filter(entry -> entry.getValue().isActive && (currentTime - entry.getValue().lastUsed) > unloadThreshold)
                .forEach(entry -> {
                    String extensionName = entry.getKey();
                    Plugin plugin = Bukkit.getPluginManager().getPlugin(extensionName);
                    if (plugin != null) {
                        Bukkit.getPluginManager().disablePlugin(plugin);
                        extensions.remove(extensionName);
                        extensionMetadata.remove(extensionName);
                        logger.info("Unloaded idle extension: " + extensionName);
                    }
                });

        enforceMaxLoaded();
    }

    private void enforceMaxLoaded() {
        if (extensions.size() <= maxLoaded) return;

        extensionMetadata.entrySet().stream()
                .filter(entry -> entry.getValue().isActive)
                .sorted((e1, e2) -> Long.compare(e1.getValue().lastUsed, e2.getValue().lastUsed))
                .limit(extensions.size() - maxLoaded)
                .forEach(entry -> {
                    String extensionName = entry.getKey();
                    Plugin plugin = Bukkit.getPluginManager().getPlugin(extensionName);
                    if (plugin != null) {
                        Bukkit.getPluginManager().disablePlugin(plugin);
                        extensions.remove(extensionName);
                        extensionMetadata.remove(extensionName);
                        logger.info("Unloaded extension to enforce max-loaded: " + extensionName);
                    }
                });
    }

    public @NotNull Map<String, ExtensionInfo> getExtensions() {
        return new HashMap<>(extensions);
    }

    public @NotNull Map<String, File> getPendingExtensions() {
        return new HashMap<>(pendingExtensions);
    }
}