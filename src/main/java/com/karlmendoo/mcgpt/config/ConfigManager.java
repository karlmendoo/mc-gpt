package com.karlmendoo.mcgpt.config;

import com.karlmendoo.mcgpt.McgptPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ConfigManager {

    private final McgptPlugin plugin;

    private String triggerKeyword;
    private boolean broadcastToAll;
    private String aiPrefix;
    private String model;
    private double temperature;
    private int maxTokens;
    private int cooldownSeconds;
    private int globalCooldownSeconds;
    private int timeoutSeconds;
    private boolean includeChatContext;
    private int contextMessageCount;
    private String apiKeySource;
    private String openaiApiKey;
    private String systemPrompt;
    private boolean logRequests;
    private String requestStackingMode;
    private int maxReplyLength;

    private final LinkedList<String> chatContextBuffer = new LinkedList<>();

    public ConfigManager(McgptPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        triggerKeyword = cfg.getString("triggerKeyword", "!ai");
        broadcastToAll = cfg.getBoolean("broadcastToAll", true);
        aiPrefix = cfg.getString("aiPrefix", "&b[AI]&f ");
        model = cfg.getString("model", "gpt-4o-mini");
        temperature = cfg.getDouble("temperature", 0.7);
        maxTokens = cfg.getInt("maxTokens", 200);
        cooldownSeconds = cfg.getInt("cooldownSeconds", 10);
        globalCooldownSeconds = cfg.getInt("globalCooldownSeconds", 0);
        timeoutSeconds = cfg.getInt("timeoutSeconds", 20);
        includeChatContext = cfg.getBoolean("includeChatContext", false);
        contextMessageCount = cfg.getInt("contextMessageCount", 10);
        apiKeySource = cfg.getString("apiKeySource", "ENV_OR_CONFIG");
        openaiApiKey = cfg.getString("openaiApiKey", "");
        systemPrompt = cfg.getString("systemPrompt",
                "You are a helpful Minecraft assistant. Keep responses concise and relevant to Minecraft.");
        logRequests = cfg.getBoolean("logRequests", false);
        requestStackingMode = cfg.getString("requestStackingMode", "REJECT").toUpperCase();
        maxReplyLength = cfg.getInt("maxReplyLength", 256);

        synchronized (chatContextBuffer) {
            chatContextBuffer.clear();
        }
    }

    public String getApiKey() {
        if ("ENV_OR_CONFIG".equalsIgnoreCase(apiKeySource)) {
            String envKey = System.getenv("OPENAI_API_KEY");
            if (envKey != null && !envKey.isBlank()) {
                return envKey;
            }
        }
        return openaiApiKey;
    }

    public void addChatContext(String message) {
        if (!includeChatContext) return;
        synchronized (chatContextBuffer) {
            chatContextBuffer.addLast(message);
            while (chatContextBuffer.size() > contextMessageCount) {
                chatContextBuffer.removeFirst();
            }
        }
    }

    public List<String> getChatContext() {
        synchronized (chatContextBuffer) {
            return Collections.unmodifiableList(new ArrayList<>(chatContextBuffer));
        }
    }

    public String getTriggerKeyword() { return triggerKeyword; }
    public boolean isBroadcastToAll() { return broadcastToAll; }
    public String getAiPrefix() { return aiPrefix; }
    public String getModel() { return model; }
    public double getTemperature() { return temperature; }
    public int getMaxTokens() { return maxTokens; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public int getGlobalCooldownSeconds() { return globalCooldownSeconds; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public boolean isIncludeChatContext() { return includeChatContext; }
    public int getContextMessageCount() { return contextMessageCount; }
    public String getSystemPrompt() { return systemPrompt; }
    public boolean isLogRequests() { return logRequests; }
    public String getRequestStackingMode() { return requestStackingMode; }
    public int getMaxReplyLength() { return maxReplyLength; }
}
