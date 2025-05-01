package com.ninja.ghast.ghastCore;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LogManager {
    private final Logger logger;
    private final String logLevel;

    public LogManager(Logger logger, FileConfiguration config) {
        this.logger = logger;
        this.logLevel = config.getString("logging.level", "INFO").toUpperCase();
    }

    public void info(String message) {
        if (logLevel.equals("INFO") || logLevel.equals("DEBUG")) {
            logger.info(message);
        }
    }

    public void warning(String message) {
        if (logLevel.equals("WARN") || logLevel.equals("INFO") || logLevel.equals("DEBUG")) {
            logger.warning(message);
        }
    }

    public void severe(String message) {
        logger.severe(message);
    }
}