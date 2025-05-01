package com.ninja.ghast.ghastCore;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CoreTabCompleter implements TabCompleter {
    private final ExtensionManager extensionManager;

    public CoreTabCompleter(ExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "list", "register", "reload", "unregister", "check", "load", "scan", "load-all"));
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("unregister") || args[0].equalsIgnoreCase("check"))) {
            completions.addAll(extensionManager.getExtensions().keySet());
            return filterCompletions(completions, args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("load")) {
            completions.addAll(extensionManager.getPendingExtensions().keySet());
            return filterCompletions(completions, args[1]);
        }

        return Collections.emptyList();
    }

    private List<String> filterCompletions(List<String> completions, String input) {
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (input.isEmpty() || completion.toLowerCase().startsWith(input.toLowerCase())) {
                filtered.add(completion);
            }
        }
        Collections.sort(filtered);
        return filtered;
    }
}