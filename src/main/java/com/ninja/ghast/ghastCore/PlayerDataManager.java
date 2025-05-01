package com.ninja.ghast.ghastCore;

import java.util.UUID;

public interface PlayerDataManager {
    String getData(UUID playerId, String namespace, String key);
    void storeData(UUID playerId, String namespace, String key, String value);
    void clearCache(UUID playerId);
}