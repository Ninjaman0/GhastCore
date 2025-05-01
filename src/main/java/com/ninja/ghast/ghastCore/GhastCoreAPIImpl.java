package com.ninja.ghast.ghastCore;

import com.ninja.ghast.ghastCore.api.GhastCoreAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

public class GhastCoreAPIImpl implements GhastCoreAPI {
    private final DatabaseManager dbManager;
    private final LogManager logger;

    public GhastCoreAPIImpl(DatabaseManager dbManager, JavaPlugin plugin) {
        this.dbManager = dbManager;
        this.logger = new LogManager(plugin.getLogger(), plugin.getConfig());
    }

    @Override
    public void storePlayerData(String playerUUID, String namespace, String key, String value) {
        dbManager.storePlayerData(playerUUID, namespace, key, value);
    }

    @Override
    public String getPlayerData(String playerUUID, String namespace, String key) {
        return dbManager.getPlayerData(playerUUID, namespace, key);
    }

    @Override
    public void clearPlayerCache(UUID playerId) {
        GhastCore.getInstance().getPlayerDataManager().clearCache(playerId);
    }

    @Override
    public Map<String, ExtensionInfo> getLoadedExtensions() {
        return GhastCore.getInstance().getExtensionManager().getExtensions();
    }

    @Override
    public void logInfo(String message) {
        logger.info(message);
    }

    @Override
    public void logWarning(String message) {
        logger.warning(message);
    }

    @Override
    public void logSevere(String message) {
        logger.severe(message);
    }
}