package com.ninja.ghast.ghastCore;

import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

public class ExtensionManager {
    private final GhastCore plugin;
    private final File extensionsFolder;
    private final Map<String, ExtensionInfo> extensions;
    private final Map<String, Long> lastUsed;

    public ExtensionManager(GhastCore plugin) {
        this.plugin = plugin;
        this.extensionsFolder = new File(plugin.getDataFolder(), "extensions");
        this.extensions = new ConcurrentHashMap<>();
        this.lastUsed = new ConcurrentHashMap<>();
        if (!extensionsFolder.exists()) {
            extensionsFolder.mkdirs();
        }
    }

    public void scanExtensions() {
        List<File> files = scanExtensionFiles();
        if (files.isEmpty()) {
            plugin.getLogger().info("No extensions found in " + extensionsFolder.getPath());
            return;
        }
        for (File file : files) {
            plugin.getLogger().info("Scanned extension: " + file.getName());
        }
    }

    public void loadAllExtensions() {
        List<File> files = scanExtensionFiles();
        if (files.isEmpty()) {
            plugin.getLogger().info("No extensions found in " + extensionsFolder.getPath());
            return;
        }

        boolean continueOnError = plugin.getConfig().getBoolean("extensions.continue-on-error", true);
        int maxLoaded = plugin.getConfig().getInt("extensions.max-loaded", 15);
        PluginManager pm = plugin.getServer().getPluginManager();

        for (File file : files) {
            if (extensions.size() >= maxLoaded) {
                plugin.getLogger().warning("Reached maximum extension limit of " + maxLoaded);
                break;
            }
            try {
                // Check if the plugin is already loaded
                String pluginName = getPluginNameFromJar(file);
                if (pluginName != null && pm.getPlugin(pluginName) != null) {
                    plugin.getLogger().info("Extension " + file.getName() + " is already loaded, skipping");
                    continue;
                }

                Plugin ext = pm.loadPlugin(file);
                if (ext instanceof ExtensionBase) {
                    pm.enablePlugin(ext); // Enable plugin to initialize ExtensionInfo
                    ExtensionInfo info = ((ExtensionBase) ext).getExtensionInfo();
                    if (info == null) {
                        plugin.getLogger().warning("Extension " + file.getName() + " returned null ExtensionInfo, skipping");
                        pm.disablePlugin(ext);
                        continue;
                    }
                    registerExtension((ExtensionBase) ext, info);
                    plugin.getLogger().info("Loaded extension: " + info.name);
                } else {
                    plugin.getLogger().warning("File " + file.getName() + " is not a valid GhastCore extension");
                    pm.disablePlugin(ext);
                }
            } catch (InvalidPluginException | InvalidDescriptionException e) {
                if (!continueOnError) {
                    throw new RuntimeException("Failed to load extension: " + file.getName(), e);
                }
                plugin.getLogger().severe("Failed to load extension: " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    private String getPluginNameFromJar(File file) {
        try (JarFile jar = new JarFile(file)) {
            var pluginYml = jar.getEntry("plugin.yml");
            if (pluginYml != null) {
                var stream = jar.getInputStream(pluginYml);
                var description = new org.bukkit.plugin.PluginDescriptionFile(stream);
                return description.getName();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read plugin.yml from " + file.getName());
        }
        return null;
    }

    private List<File> scanExtensionFiles() {
        List<File> jarFiles = new ArrayList<>();
        boolean recursiveScan = plugin.getConfig().getBoolean("extensions.recursive-scan", false);
        int maxScanDepth = plugin.getConfig().getInt("extensions.max-scan-depth", 3);
        scanDirectory(extensionsFolder, jarFiles, recursiveScan, maxScanDepth, 0);
        return jarFiles;
    }

    private void scanDirectory(File directory, List<File> jarFiles, boolean recursive, int maxDepth, int currentDepth) {
        if (!directory.isDirectory() || currentDepth > maxDepth) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
                try (JarFile jar = new JarFile(file)) {
                    jarFiles.add(file);
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid extension JAR: " + file.getName());
                }
            } else if (recursive && file.isDirectory()) {
                scanDirectory(file, jarFiles, recursive, maxDepth, currentDepth + 1);
            }
        }
    }

    public void unloadIdleExtensions() {
        long unloadAfter = plugin.getConfig().getLong("extensions.unload-after-minutes", 30) * 60 * 1000L;
        PluginManager pm = plugin.getServer().getPluginManager();

        lastUsed.forEach((name, lastUsedTime) -> {
            if (System.currentTimeMillis() - lastUsedTime > unloadAfter) {
                ExtensionInfo info = extensions.get(name);
                if (info != null) {
                    Plugin plugin = pm.getPlugin(info.name);
                    if (plugin != null) {
                        pm.disablePlugin(plugin);
                        extensions.remove(name);
                        lastUsed.remove(name);
                        this.plugin.getLogger().info("Unloaded idle extension: " + name);
                    }
                }
            }
        });
    }

    public void registerExtension(ExtensionBase extension, ExtensionInfo info) {
        extensions.put(info.name.toLowerCase(), info);
        lastUsed.put(info.name.toLowerCase(), System.currentTimeMillis());
    }

    public void unregisterExtension(String name) {
        extensions.remove(name.toLowerCase());
        lastUsed.remove(name.toLowerCase());
    }

    public Map<String, ExtensionInfo> getExtensions() {
        return new HashMap<>(extensions);
    }

    public boolean loadExtension(String name) {
        // Placeholder for specific extension loading
        return false;
    }

    public void loadAllPending() {
        loadAllExtensions();
    }

    public Map<String, ExtensionInfo> getPendingExtensions() {
        // Placeholder for pending extensions
        return new HashMap<>();
    }
}