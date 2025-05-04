package com.ninja.ghast.ghastCore;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CoreCommand implements CommandExecutor {
    private final GhastCore plugin;
    private final ExtensionManager extensionManager;

    public CoreCommand(GhastCore plugin, ExtensionManager extensionManager) {
        this.plugin = plugin;
        this.extensionManager = extensionManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou must be an operator to use this command.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§6GhastCore Commands:");
            sender.sendMessage("§e/gcore help §7- Show this help message");
            sender.sendMessage("§e/gcore list §7- List loaded and pending extensions");
            sender.sendMessage("§e/gcore register §7- Register new extensions");
            sender.sendMessage("§e/gcore reload §7- Reload configuration and extensions");
            sender.sendMessage("§e/gcore unregister <extension> §7- Unregister an extension");
            sender.sendMessage("§e/gcore check <extension> §7- Check extension info");
            sender.sendMessage("§e/gcore load <extension> §7- Load a lazy-loaded extension");
            sender.sendMessage("§e/gcore scan §7- Rescan extension folder");
            sender.sendMessage("§e/gcore load-all §7- Load all pending extensions");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                sender.sendMessage("§6Loaded Extensions:");
                for (ExtensionInfo info : extensionManager.getExtensions().values()) {
                    sender.sendMessage("§e- " + info.name + " v" + info.version +
                            " by " + info.authors);
                }
                if (plugin.getConfig().getBoolean("extensions.lazy-load", false)) {
                    sender.sendMessage("§6Pending Extensions:");
                    for (String name : extensionManager.getPendingExtensions().keySet()) {
                        sender.sendMessage("§e- " + name);
                    }
                }
                return true;

            case "register":
                extensionManager.loadAllPending();
                sender.sendMessage("§aExtensions registered");
                return true;

            case "reload":
                try {
                    plugin.reloadConfig();
                    plugin.getLogger().info("Configuration reloaded");
                    plugin.getDatabaseManager().closeConnection();
                    plugin.setDatabaseManager(new DatabaseManager(plugin));
                    plugin.getLogger().info("Database connection pool reinitialized");
                    extensionManager.loadAllPending();
                    plugin.getLogger().info("Extensions reloaded");
                    sender.sendMessage("§aGhastCore reloaded");
                } catch (Exception e) {
                    plugin.getLogger().severe("Error reloading GhastCore: " + e.getMessage());
                    sender.sendMessage("§cError reloading GhastCore: " + e.getMessage());
                }
                return true;

            case "unregister":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /gcore unregister <extension>");
                    return true;
                }
                extensionManager.unregisterExtension(args[1]);
                sender.sendMessage("§aUnregistered extension: " + args[1]);
                return true;

            case "check":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /gcore check <extension>");
                    return true;
                }
                ExtensionInfo info = extensionManager.getExtensions().get(args[1].toLowerCase());
                if (info != null) {
                    sender.sendMessage("§6Extension Info:");
                    sender.sendMessage("§eName: §f" + info.name);
                    sender.sendMessage("§eAuthors: §f" + info.authors);
                    sender.sendMessage("§eVersion: §f" + info.version);
                    sender.sendMessage("§eNamespace: §f" + info.namespace);
                    sender.sendMessage("§eStatus: §aRegistered");
                } else {
                    sender.sendMessage("§cExtension not found: " + args[1]);
                }
                return true;

            case "load":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /gcore load <extension>");
                    return true;
                }
                if (extensionManager.loadExtension(args[1])) {
                    sender.sendMessage("§aLoaded extension: " + args[1]);
                } else {
                    sender.sendMessage("§cFailed to load extension: " + args[1]);
                }
                return true;

            case "scan":
                extensionManager.scanExtensions();
                sender.sendMessage("§aExtension folder rescanned");
                return true;

            case "load-all":
                extensionManager.loadAllPending();
                sender.sendMessage("§aAll pending extensions loaded");
                return true;

            default:
                sender.sendMessage("§cUnknown command. Use /gcore help");
                return true;
        }
    }
}