package com.mcgpt.command;

import com.mcgpt.McgptPlugin;
import java.util.Collections;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AiCommand implements CommandExecutor {

    private final McgptPlugin plugin;

    public AiCommand(McgptPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    "Usage: /ai <message> or /ai reload"));
            return true;
        }

        // /ai reload
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("chatgpt.admin")) {
                plugin.sendMessage(sender, "&cYou don't have permission to do that.");
                return true;
            }
            plugin.getConfigManager().reload();
            plugin.sendMessage(sender, "&aConfiguration reloaded successfully.");
            return true;
        }

        // /ai <message>
        if (!sender.hasPermission("chatgpt.use")) {
            plugin.sendMessage(sender, "&cYou don't have permission to use the AI.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            // Allow console use
            String message = String.join(" ", args);
            plugin.getLogger().info("[McGPT] Console requested AI: " + message);
            plugin.getGeminiClient().ask(message, Collections.emptyList(), "").thenAccept(reply -> {
                String cleaned = plugin.cleanReply(reply);
                plugin.getLogger().info("[McGPT] AI reply: " + cleaned);
            }).exceptionally(ex -> {
                plugin.getLogger().warning("[McGPT] AI request failed: " + ex.getMessage());
                return null;
            });
            return true;
        }

        String message = String.join(" ", args);
        plugin.handleAiRequest(player, message);
        return true;
    }
}
