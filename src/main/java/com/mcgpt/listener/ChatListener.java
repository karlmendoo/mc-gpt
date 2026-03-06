package com.mcgpt.listener;

import com.mcgpt.McgptPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {

    private final McgptPlugin plugin;

    public ChatListener(McgptPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String plainText = PlainTextComponentSerializer.plainText().serialize(event.message());
        String keyword = plugin.getConfigManager().getTriggerKeyword();

        if (!plainText.startsWith(keyword)) {
            // Track context for all messages if enabled
            if (plugin.getConfigManager().isIncludeChatContext()) {
                plugin.getChatContextManager().addMessage(
                        event.getPlayer().getUniqueId(), plainText);
            }
            return;
        }

        // Cancel the chat event so the raw trigger isn't shown in public chat
        event.setCancelled(true);

        // Extract the message part after the keyword
        String message = plainText.substring(keyword.length()).trim();
        if (message.isEmpty()) {
            plugin.sendMessage(event.getPlayer(), "&cPlease provide a message after " + keyword);
            return;
        }

        plugin.handleAiRequest(event.getPlayer(), message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getCooldownManager().clearPending(event.getPlayer().getUniqueId());
    }
}
