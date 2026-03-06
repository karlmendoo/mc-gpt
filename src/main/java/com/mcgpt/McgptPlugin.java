package com.mcgpt;

import com.mcgpt.api.GeminiClient;
import com.mcgpt.command.AiCommand;
import com.mcgpt.config.ConfigManager;
import com.mcgpt.listener.ChatListener;
import com.mcgpt.manager.ChatContextManager;
import com.mcgpt.manager.CooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class McgptPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private CooldownManager cooldownManager;
    private ChatContextManager chatContextManager;
    private GeminiClient geminiClient;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);

        String apiKey = configManager.getGeminiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            getLogger().severe("=================================================");
            getLogger().severe("[McGPT] No Gemini API key found!");
            getLogger().severe("[McGPT] Set the GEMINI_API_KEY environment variable");
            getLogger().severe("[McGPT] or set 'geminiApiKey' in config.yml.");
            getLogger().severe("[McGPT] The plugin will be disabled.");
            getLogger().severe("=================================================");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        cooldownManager = new CooldownManager();
        chatContextManager = new ChatContextManager(configManager.getContextMessageCount());
        geminiClient = new GeminiClient(configManager, getLogger());

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        AiCommand aiCommand = new AiCommand(this);
        if (getCommand("ai") != null) {
            getCommand("ai").setExecutor(aiCommand);
        }

        getLogger().info("[McGPT] Plugin enabled. Trigger keyword: "
                + configManager.getTriggerKeyword());
    }

    @Override
    public void onDisable() {
        getLogger().info("[McGPT] Plugin disabled.");
    }

    /**
     * Handles an AI request from a player, applying cooldown and pending checks.
     */
    public void handleAiRequest(Player player, String message) {
        UUID uuid = player.getUniqueId();

        // Global cooldown check
        int globalRemaining = cooldownManager.getGlobalCooldownRemaining();
        if (globalRemaining > 0) {
            sendMessage(player, "&cThe AI is on global cooldown. Please wait "
                    + globalRemaining + " second(s).");
            return;
        }

        // Per-player cooldown check
        int playerRemaining = cooldownManager.getPlayerCooldownRemaining(
                uuid, configManager.getCooldownSeconds());
        if (playerRemaining > 0) {
            sendMessage(player, "&cYou must wait " + playerRemaining
                    + " second(s) before using the AI again.");
            return;
        }

        // Pending request check
        if (!cooldownManager.tryMarkPending(uuid)) {
            sendMessage(player, "&cYour previous request is still processing. Please wait.");
            return;
        }

        // Record usage
        cooldownManager.recordUse(uuid, configManager.getCooldownSeconds(),
                configManager.getGlobalCooldownSeconds());

        // Gather context if enabled
        String context = "";
        if (configManager.isIncludeChatContext()) {
            context = chatContextManager.getContext(uuid);
        }

        if (configManager.isEnableLogging()) {
            getLogger().info("[McGPT] " + player.getName() + " asked: " + message);
        }

        final String finalContext = context;
        geminiClient.ask(message, finalContext)
                .thenAccept(reply -> {
                    String cleaned = cleanReply(reply);
                    if (configManager.isEnableLogging()) {
                        getLogger().info("[McGPT] AI replied: " + cleaned);
                    }
                    Component prefix = translateColors(configManager.getAiPrefix());
                    Component replyComponent = prefix.append(Component.text(cleaned));
                    broadcastOrSend(player, replyComponent);
                })
                .exceptionally(ex -> {
                    getLogger().warning("[McGPT] Gemini request failed: " + ex.getMessage());
                    sendMessage(player, "&cSorry, the AI is unavailable right now. Please try again later.");
                    return null;
                })
                .whenComplete((v, t) -> cooldownManager.clearPending(uuid));
    }

    /**
     * Sends a reply to all players or only the requester based on config.
     */
    private void broadcastOrSend(Player player, Component message) {
        if (configManager.isBroadcastToAll()) {
            Bukkit.getServer().broadcast(message);
        } else {
            player.sendMessage(message);
        }
    }

    /**
     * Sanitizes and truncates the AI reply.
     */
    public String cleanReply(String reply) {
        if (reply == null) return "";
        // Replace newlines with spaces
        String cleaned = reply.replace("\n", " ").replace("\r", "").trim();
        // Strip § and & color codes from AI reply content to prevent injection before truncation
        cleaned = cleaned.replace("§", "").replace("&", "");
        // Limit length
        int maxLen = configManager.getMaxReplyLength();
        if (cleaned.length() > maxLen) {
            cleaned = cleaned.substring(0, maxLen) + "...";
        }
        return cleaned;
    }

    /**
     * Translates legacy & color codes to an Adventure Component.
     */
    public Component translateColors(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    /**
     * Sends a player a message with & color code support.
     */
    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(translateColors(message));
    }

    public ConfigManager getConfigManager() { return configManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public ChatContextManager getChatContextManager() { return chatContextManager; }
    public GeminiClient getGeminiClient() { return geminiClient; }
}
