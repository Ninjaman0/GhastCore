package com.ninja.ghast.ghastCore.api;

import com.ninja.ghast.ghastCore.ExtensionInfo;

import java.util.Map;
import java.util.UUID;

/**
 * The public API for GhastCore, providing access to player data management and logging.
 */
public interface GhastCoreAPI {
    /**
     * Stores player data in the database.
     * @param playerUUID The UUID of the player.
     * @param namespace The namespace for the data.
     * @param key The key for the data.
     * @param value The value to store.
     */
    void storePlayerData(String playerUUID, String namespace, String key, String value);

    /**
     * Retrieves player data from the database.
     * @param playerUUID The UUID of the player.
     * @param namespace The namespace for the data.
     * @param key The key for the data.
     * @return The stored value or null if not found.
     */
    String getPlayerData(String playerUUID, String namespace, String key);

    /**
     * Clears the cache for a specific player.
     * @param playerId The UUID of the player.
     */
    void clearPlayerCache(UUID playerId);

    /**
     * Retrieves a map of loaded extensions.
     * @return A map of extension names to their metadata.
     */
    Map<String, ExtensionInfo> getLoadedExtensions();

    /**
     * Logs an informational message.
     * @param message The message to log.
     */
    void logInfo(String message);

    /**
     * Logs a warning message.
     * @param message The message to log.
     */
    void logWarning(String message);

    /**
     * Logs a severe error message.
     * @param message The message to log.
     */
    void logSevere(String message);
}