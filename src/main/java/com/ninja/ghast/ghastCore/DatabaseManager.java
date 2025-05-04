package com.ninja.ghast.ghastCore;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private final LogManager logger;
    private HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = new LogManager(plugin.getLogger(), plugin.getConfig());
        initializeDatabase();
    }

    private void initializeDatabase() {
        FileConfiguration config = plugin.getConfig();
        String dbType = config.getString("database.type", "sqlite").toLowerCase();

        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setMaximumPoolSize(config.getInt("database.pool.max-size", 10));
            hikariConfig.setMaxLifetime(config.getLong("database.pool.idle-timeout", 30000));
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setConnectionTestQuery("SELECT 1");
            hikariConfig.setValidationTimeout(5000); // 5 seconds
            hikariConfig.setLeakDetectionThreshold(30000); // Detect leaks after 30s

            if (dbType.equals("mysql")) {
                String host = config.getString("database.mysql.host");
                String port = config.getString("database.mysql.port");
                String database = config.getString("database.mysql.database");
                String username = config.getString("database.mysql.username");
                String password = config.getString("database.mysql.password");

                if (host == null) throw new IllegalArgumentException("MySQL host not configured");
                if (port == null) throw new IllegalArgumentException("MySQL port not configured");
                if (database == null) throw new IllegalArgumentException("MySQL database not configured");
                if (username == null) throw new IllegalArgumentException("MySQL username not configured");
                if (password == null) throw new IllegalArgumentException("MySQL password not configured");

                hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database +
                        "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
                hikariConfig.setUsername(username);
                hikariConfig.setPassword(password);
            } else {
                hikariConfig.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder() + "/data.db");
            }

            dataSource = new HikariDataSource(hikariConfig);

            boolean tableExists = false;
            try (Connection conn = getConnection();
                 ResultSet rs = conn.getMetaData().getTables(null, null, "player_data", null)) {
                if (rs.next()) {
                    tableExists = true;
                }
            }

            if (!tableExists) {
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "CREATE TABLE player_data (" +
                                     "player_uuid VARCHAR(36) NOT NULL," +
                                     "namespace VARCHAR(100) NOT NULL," +
                                     "key_name VARCHAR(100) NOT NULL," +
                                     "value TEXT," +
                                     "PRIMARY KEY (player_uuid, namespace, key_name)" +
                                     ")")) {
                    stmt.execute();
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
            throw new IllegalStateException("Database initialization failed", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("Database connection pool not initialized");
        }
        return dataSource.getConnection();
    }

    public void storePlayerData(String playerUUID, String namespace, String key, String value) {
        String sql = "INSERT OR REPLACE INTO player_data (player_uuid, namespace, key_name, value) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID);
            stmt.setString(2, namespace);
            stmt.setString(3, key);
            stmt.setString(4, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error storing player data: " + e.getMessage());
            throw new IllegalStateException("Database operation failed", e);
        }
    }

    public String getPlayerData(String playerUUID, String namespace, String key) {
        String sql = "SELECT value FROM player_data WHERE player_uuid = ? AND namespace = ? AND key_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID);
            stmt.setString(2, namespace);
            stmt.setString(3, key);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("value");
            }
        } catch (SQLException e) {
            logger.severe("Error retrieving player data: " + e.getMessage());
            throw new IllegalStateException("Database operation failed", e);
        }
        return null;
    }

    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }
}