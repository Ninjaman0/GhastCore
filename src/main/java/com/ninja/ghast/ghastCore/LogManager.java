package com.ninja.ghast.ghastCore;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LogManager {
    private final Logger logger;
    private final Level logLevel;

    public LogManager(Logger logger, FileConfiguration config) {
        this.logger = logger;
        String levelStr = config.getString("logging.level", "INFO").toUpperCase();
        this.logLevel = Level.parse(levelStr);
    }

    public void debug(String message) {
        if (logLevel.intValue() <= Level.FINE.intValue()) {
            logger.fine(message);
        }
    }

    public void info(String message) {
        if (logLevel.intValue() <= Level.INFO.intValue()) {
            logger.info(message);
        }
    }

    public void warning(String message) {
        if (logLevel.intValue() <= Level.WARNING.intValue()) {
            logger.warning(message);
        }
    }

    public void severe(String message) {
        if (logLevel.intValue() <= Level.SEVERE.intValue()) {
            logger.severe(message);
        }
    }

    public void log(Level level, String message, Throwable thrown) {
        if (logLevel.intValue() <= level.intValue()) {
            logger.log(level, message, thrown);
        }
    }
}