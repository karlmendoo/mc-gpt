package com.karlmendoo.mcgpt.listener;

import com.karlmendoo.mcgpt.McgptPlugin;
import com.karlmendoo.mcgpt.config.ConfigManager;
import com.karlmendoo.mcgpt.cooldown.CooldownManager;
import com.karlmendoo.mcgpt.openai.OpenAiClient;
import com.karlmendoo.mcgpt.util.ChatFormatter;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.UUID;

public class ChatListener implements Listener {

    private final McgptPlugin plugin;

    public ChatListener(McgptPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ConfigManager cfg = plugin.getConfigManager();

        // Serialize the chat message component to plain text
        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        String keyword = cfg.getTriggerKeyword();

        // Check if the message starts with the trigger keyword
        if (!rawMessage.startsWith(keyword)) {
            // Not an AI trigger — record for context if enabled
            if (cfg.isIncludeChatContext()) {
                String contextEntry = player.getName() + ": " + ChatFormatter.sanitizeInput(rawMessage);
                cfg.addChatContext(contextEntry);
            }
            return;
        }

        // Cancel the original message so "!ai ..." doesn't show in chat
        event.setCancelled(true);

        // Check permission
        if (!player.hasPermission("chatgpt.use")) {
            sendMessage(player, "&cYou don't have permission to use the AI feature.");
            return;
        }

        // Extract the user's query (everything after the keyword + optional space)
        String query = rawMessage.substring(keyword.length()).trim();
        if (query.isEmpty()) {
            sendMessage(player, "&cUsage: " + keyword + " <your question>");
            return;
        }

        handleAiRequest(player, query);
    }

    public void handleAiRequest(Player player, String rawQuery) {
        ConfigManager cfg = plugin.getConfigManager();
        CooldownManager cooldowns = plugin.getCooldownManager();
        UUID playerId = player.getUniqueId();

        // Check pending request
        if (cooldowns.isPending(playerId)) {
            if ("REJECT".equals(cfg.getRequestStackingMode())) {
                sendMessage(player, "&ePlease wait for the current response.");
                return;
            }
            // QUEUE mode falls through — they just queue another request
        }

        // Check per-player cooldown
        if (cooldowns.isOnCooldown(playerId, cfg.getCooldownSeconds())) {
            int remaining = cooldowns.getRemainingCooldown(playerId);
            sendMessage(player, "&ePlease wait &6" + remaining + "&e second(s) before sending another AI request.");
            return;
        }

        // Check global cooldown
        if (cooldowns.isGlobalCooldownActive(cfg.getGlobalCooldownSeconds())) {
            int remaining = cooldowns.getRemainingGlobalCooldown();
            sendMessage(player, "&eThe AI is on a global cooldown. Please wait &6" + remaining + "&e second(s).");
            return;
        }

        String sanitizedQuery = ChatFormatter.sanitizeInput(rawQuery);
        if (sanitizedQuery.isEmpty()) {
            sendMessage(player, "&cPlease provide a valid question.");
            return;
        }

        // Apply cooldowns and mark pending
        cooldowns.setCooldown(playerId, cfg.getCooldownSeconds());
        cooldowns.setGlobalCooldown(cfg.getGlobalCooldownSeconds());
        cooldowns.markPending(playerId);

        List<String> context = cfg.getChatContext();
        OpenAiClient client = plugin.getOpenAiClient();

        client.sendMessage(sanitizedQuery, context)
                .thenAccept(reply -> {
                    cooldowns.clearPending(playerId);
                    String sanitized = ChatFormatter.sanitizeReply(reply, cfg.getMaxReplyLength());
                    String prefix = ChatFormatter.colorize(cfg.getAiPrefix());
                    String fullMessage = prefix + sanitized;

                    // Schedule broadcasting on the main thread
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (cfg.isBroadcastToAll()) {
                            plugin.getServer().broadcastMessage(fullMessage);
                        } else {
                            if (player.isOnline()) {
                                player.sendMessage(fullMessage);
                            }
                        }
                    });
                })
                .exceptionally(ex -> {
                    cooldowns.clearPending(playerId);
                    plugin.getLogger().severe("[McGPT] Error processing AI request: " + ex.getMessage());
                    if (ex.getCause() != null) {
                        ex.getCause().printStackTrace();
                    } else {
                        ex.printStackTrace();
                    }
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            player.sendMessage(ChatFormatter.colorize("&cSorry, the AI is currently unavailable. Please try again later."));
                        }
                    });
                    return null;
                });
    }

    private void sendMessage(Player player, String message) {
        String colored = ChatFormatter.colorize(message);
        // AsyncChatEvent is already off the main thread; we need to schedule Bukkit API calls
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.sendMessage(colored);
            }
        });
    }
}
