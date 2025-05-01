package com.ninja.ghast.ghastCore;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PlayerDataManagerImpl implements PlayerDataManager, Listener {
    private final DatabaseManager databaseManager;
    private final Map<UUID, Map<String, String>> dataCache;
    private final boolean cachingEnabled;
    private final int cacheTTL;
    private final Map<UUID, Long> lastAccessTimes;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final LogManager logger;

    public PlayerDataManagerImpl(DatabaseManager databaseManager, boolean cachingEnabled, int cacheTTL) {
        this.databaseManager = databaseManager;
        this.cachingEnabled = cachingEnabled;
        this.cacheTTL = cacheTTL;
        this.dataCache = new ConcurrentHashMap<>();
        this.lastAccessTimes = new ConcurrentHashMap<>();
        this.logger = new LogManager(GhastCore.getInstance().getLogger(), GhastCore.getInstance().getConfig());
        startCacheEvictionTask();
    }

    @Override
    public String getData(UUID playerId, String namespace, String key) {
        String cacheKey = namespace + "." + key;

        lock.readLock().lock();
        try {
            if (cachingEnabled && dataCache.containsKey(playerId)) {
                String value = dataCache.get(playerId).get(cacheKey);
                if (value != null) {
                    lastAccessTimes.put(playerId, System.currentTimeMillis());
                    return value;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        String value = databaseManager.getPlayerData(playerId.toString(), namespace, key);

        if (cachingEnabled && value != null) {
            lock.writeLock().lock();
            try {
                dataCache.computeIfAbsent(playerId, k -> new HashMap<>()).put(cacheKey, value);
                lastAccessTimes.put(playerId, System.currentTimeMillis());
            } finally {
                lock.writeLock().unlock();
            }
        }
        return value;
    }

    @Override
    public void storeData(UUID playerId, String namespace, String key, String value) {
        databaseManager.storePlayerData(playerId.toString(), namespace, key, value);

        if (cachingEnabled) {
            lock.writeLock().lock();
            try {
                dataCache.computeIfAbsent(playerId, k -> new HashMap<>()).put(namespace + "." + key, value);
                lastAccessTimes.put(playerId, System.currentTimeMillis());
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public void clearCache(UUID playerId) {
        lock.writeLock().lock();
        try {
            dataCache.remove(playerId);
            lastAccessTimes.remove(playerId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lock.writeLock().lock();
        try {
            lastAccessTimes.put(playerId, System.currentTimeMillis());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        clearCache(playerId);
    }

    private void startCacheEvictionTask() {
        if (!cachingEnabled) return;

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(cacheTTL * 1000L);
                    long currentTime = System.currentTimeMillis();
                    lock.writeLock().lock();
                    try {
                        Set<UUID> toRemove = new HashSet<>();
                        for (Map.Entry<UUID, Long> entry : lastAccessTimes.entrySet()) {
                            if (currentTime - entry.getValue() > cacheTTL * 1000L) {
                                toRemove.add(entry.getKey());
                            }
                        }
                        for (UUID playerId : toRemove) {
                            dataCache.remove(playerId);
                            lastAccessTimes.remove(playerId);
                            logger.debug("Evicted cache for player: " + playerId);
                        }
                    } finally {
                        lock.writeLock().unlock();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "GhastCore-CacheEviction").start();
    }
}