package com.ninja.ghast.ghastCore;

import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.util.HashMap;
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

    public void loadExtensions() {
        boolean asyncScan = plugin.getConfig().getBoolean("extensions.async-scan", true);
        if (asyncScan) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::scanAndLoadExtensions);
        } else {
            scanAndLoadExtensions();
        }
    }

    private void scanAndLoadExtensions() {
        File[] files = extensionsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (files == null) return;

        boolean continueOnError = plugin.getConfig().getBoolean("extensions.continue-on-error", true);
        int maxLoaded = plugin.getConfig().getInt("extensions.max-loaded", 15);
        PluginManager pm = plugin.getServer().getPluginManager();

        for (File file : files) {
            if (extensions.size() >= maxLoaded) {
                plugin.getLogger().warning("Reached maximum extension limit of " + maxLoaded);
                break;
            }
            try {
                Plugin ext = pm.loadPlugin(file);
                if (ext instanceof ExtensionBase) {
                    ExtensionInfo info = ((ExtensionBase) ext).getExtensionInfo();
                    extensions.put(info.name.toLowerCase(), info);
                    lastUsed.put(info.name.toLowerCase(), System.currentTimeMillis());
                    pm.enablePlugin(ext);
                } else {
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

    public void scanExtensions() {
        File[] files = extensionsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (files == null) return;

        for (File file : files) {
            try (JarFile jar = new JarFile(file)) {
                // Validate JAR but don't load
                plugin.getLogger().info("Scanned extension: " + file.getName());
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid extension JAR: " + file.getName());
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
}