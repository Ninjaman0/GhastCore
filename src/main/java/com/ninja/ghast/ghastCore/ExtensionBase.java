package com.ninja.ghast.ghastCore;

import com.ninja.ghast.ghastCore.api.GhastCoreAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class ExtensionBase extends JavaPlugin {
    protected GhastCoreAPI api;
    private ExtensionInfo info;

    @Override
    public void onEnable() {
        GhastCore core = GhastCore.getInstance();
        if (core == null) {
            getLogger().severe("GhastCore not found! Disabling extension.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        String version = Bukkit.getMinecraftVersion();
        if (!version.startsWith("1.20") && !version.startsWith("1.21")) {
            getLogger().severe("Unsupported Paper version: " + version + ". Disabling extension.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        info = new ExtensionInfo(
                getDescription().getName(),
                getDescription().getAuthors().isEmpty() ? "Unknown" : String.join(", ", getDescription().getAuthors()),
                getDescription().getVersion(),
                getDescription().getName().toLowerCase()
        );

        core.getExtensionManager().registerExtension(this, info);

        api = core.getAPI();

        onExtensionEnable();
        getLogger().info("Extension " + info.name + " registered with GhastCore");
    }

    @Override
    public void onDisable() {
        GhastCore core = GhastCore.getInstance();
        if (core != null) {
            core.getExtensionManager().unregisterExtension(info.name);
        }
    }

    public GhastCoreAPI getAPI() {
        return api;
    }

    public ExtensionInfo getExtensionInfo() {
        return info;
    }

    public abstract void onExtensionEnable();
}