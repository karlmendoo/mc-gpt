package com.karlmendoo.mcgpt.command;

import com.karlmendoo.mcgpt.McgptPlugin;
import com.karlmendoo.mcgpt.util.ChatFormatter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class AiCommand implements CommandExecutor, TabCompleter {

    private final McgptPlugin plugin;

    public AiCommand(McgptPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatFormatter.colorize("&cUsage: /ai <message> | /ai reload"));
            return true;
        }

        // /ai reload
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("chatgpt.admin")) {
                sender.sendMessage(ChatFormatter.colorize("&cYou don't have permission to reload the config."));
                return true;
            }
            plugin.getConfigManager().load();
            sender.sendMessage(ChatFormatter.colorize("&aMcGPT config reloaded successfully."));
            plugin.getLogger().info("[McGPT] Config reloaded by " + sender.getName());
            return true;
        }

        // /ai <message>
        if (!sender.hasPermission("chatgpt.use")) {
            sender.sendMessage(ChatFormatter.colorize("&cYou don't have permission to use the AI feature."));
            return true;
        }

        if (!(sender instanceof Player player)) {
            // Console support: send without cooldown/broadcast checks
            String query = String.join(" ", args);
            String sanitized = ChatFormatter.sanitizeInput(query);
            if (sanitized.isEmpty()) {
                sender.sendMessage(ChatFormatter.colorize("&cPlease provide a valid question."));
                return true;
            }
            plugin.getOpenAiClient()
                    .sendMessage(sanitized, Collections.emptyList())
                    .thenAccept(reply -> {
                        String sanitizedReply = ChatFormatter.sanitizeReply(
                                reply, plugin.getConfigManager().getMaxReplyLength());
                        plugin.getServer().getScheduler().runTask(plugin, () ->
                                sender.sendMessage(ChatFormatter.colorize(
                                        plugin.getConfigManager().getAiPrefix()) + sanitizedReply));
                    })
                    .exceptionally(ex -> {
                        plugin.getLogger().severe("[McGPT] Console AI request failed: " + ex.getMessage());
                        plugin.getServer().getScheduler().runTask(plugin, () ->
                                sender.sendMessage(ChatFormatter.colorize("&cSorry, the AI is currently unavailable.")));
                        return null;
                    });
            return true;
        }

        String query = String.join(" ", args);
        plugin.getChatListener().handleAiRequest(player, query);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && sender.hasPermission("chatgpt.admin")) {
            if ("reload".startsWith(args[0].toLowerCase())) {
                return List.of("reload");
            }
        }
        return Collections.emptyList();
    }
}
