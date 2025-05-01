package com.ninja.ghast.ghastCore;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManagerImpl implements PlayerDataManager, Listener {
    private final DatabaseManager dbManager;
    private final boolean cachingEnabled;
    private final int cacheTTL;
    private final Map<UUID, Map<String, Map<String, String>>> cache;
    private final Map<UUID, Long> cacheTimestamps;

    public PlayerDataManagerImpl(DatabaseManager dbManager, boolean cachingEnabled, int cacheTTL) {
        this.dbManager = dbManager;
        this.cachingEnabled = cachingEnabled;
        this.cacheTTL = cacheTTL;
        this.cache = new ConcurrentHashMap<>();
        this.cacheTimestamps = new ConcurrentHashMap<>();
    }

    @Override
    public String getData(UUID playerId, String namespace, String key) {
        if (cachingEnabled) {
            Map<String, Map<String, String>> playerCache = cache.get(playerId);
            if (playerCache != null) {
                Map<String, String> namespaceCache = playerCache.get(namespace);
                if (namespaceCache != null && namespaceCache.containsKey(key)) {
                    Long timestamp = cacheTimestamps.get(playerId);
                    if (timestamp != null && (System.currentTimeMillis() - timestamp) < cacheTTL * 1000L) {
                        return namespaceCache.get(key);
                    }
                }
            }
        }
        String value = dbManager.getPlayerData(playerId.toString(), namespace, key);
        if (cachingEnabled && value != null) {
            cache.computeIfAbsent(playerId, k -> new HashMap<>())
                    .computeIfAbsent(namespace, k -> new HashMap<>())
                    .put(key, value);
            cacheTimestamps.put(playerId, System.currentTimeMillis());
        }
        return value;
    }

    @Override
    public void storeData(UUID playerId, String namespace, String key, String value) {
        dbManager.storePlayerData(playerId.toString(), namespace, key, value);
        if (cachingEnabled) {
            cache.computeIfAbsent(playerId, k -> new HashMap<>())
                    .computeIfAbsent(namespace, k -> new HashMap<>())
                    .put(key, value);
            cacheTimestamps.put(playerId, System.currentTimeMillis());
        }
    }

    @Override
    public void clearCache(UUID playerId) {
        cache.remove(playerId);
        cacheTimestamps.remove(playerId);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearCache(event.getPlayer().getUniqueId());
    }
}