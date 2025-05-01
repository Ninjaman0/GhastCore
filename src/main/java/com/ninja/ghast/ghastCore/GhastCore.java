package com.ninja.ghast.ghastCore;

import com.ninja.ghast.ghastCore.api.GhastCoreAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GhastCore extends JavaPlugin {
    private static GhastCore instance;
    private LogManager logger;
    private DatabaseManager dbManager;
    private ExtensionManager extensionManager;
    private GhastCoreAPI api;
    private PlayerDataManager playerDataManager;
    private ScheduledExecutorService scheduler;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        instance = this;
        logger = new LogManager(getLogger(), getConfig());

        saveDefaultConfig();
        dbManager = new DatabaseManager(this);
        boolean cachingEnabled = getConfig().getBoolean("caching.enabled", true);
        int cacheTTL = getConfig().getInt("caching.flushIntervalSeconds", 300);
        playerDataManager = new PlayerDataManagerImpl(dbManager, cachingEnabled, cacheTTL);
        extensionManager = new ExtensionManager(this);
        api = new GhastCoreAPIImpl(dbManager, this);

        getServer().getPluginManager().registerEvents((PlayerDataManagerImpl) playerDataManager, this);

        if (!getConfig().getBoolean("extensions.lazy-load", false)) {
            extensionManager.loadExtensions();
        } else {
            extensionManager.scanExtensions();
        }

        getCommand("gcore").setExecutor(new CoreCommand(this, extensionManager));
        getCommand("gcore").setTabCompleter(new CoreTabCompleter(extensionManager));

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> extensionManager.unloadIdleExtensions(), 1, 1, TimeUnit.MINUTES);

        logger.info("GhastCore enabled successfully in " + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    public void onDisable() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (dbManager != null) {
            dbManager.closeConnection();
        }
        logger.info("GhastCore disabled");
    }

    public static GhastCore getInstance() {
        return instance;
    }

    public GhastCoreAPI getAPI() {
        return api;
    }

    public ExtensionManager getExtensionManager() {
        return extensionManager;
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public void setDatabaseManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
}