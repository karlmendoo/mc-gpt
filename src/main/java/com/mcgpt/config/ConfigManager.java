package com.mcgpt.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {

    private final JavaPlugin plugin;

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
    private String geminiApiKey;
    private String systemPrompt;
    private boolean enableLogging;
    private int maxReplyLength;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        load();
    }

    public void reload() {
        plugin.reloadConfig();
        load();
    }

    private void load() {
        FileConfiguration cfg = plugin.getConfig();
        triggerKeyword = cfg.getString("triggerKeyword", "!ai");
        broadcastToAll = cfg.getBoolean("broadcastToAll", true);
        aiPrefix = cfg.getString("aiPrefix", "&b[AI]&f ");
        model = cfg.getString("model", "gemini-2.0-flash");
        temperature = cfg.getDouble("temperature", 0.7);
        maxTokens = cfg.getInt("maxTokens", 200);
        cooldownSeconds = cfg.getInt("cooldownSeconds", 10);
        globalCooldownSeconds = cfg.getInt("globalCooldownSeconds", 0);
        timeoutSeconds = cfg.getInt("timeoutSeconds", 20);
        includeChatContext = cfg.getBoolean("includeChatContext", false);
        contextMessageCount = Math.max(1, cfg.getInt("contextMessageCount", 10));
        apiKeySource = cfg.getString("apiKeySource", "ENV_OR_CONFIG");
        geminiApiKey = cfg.getString("geminiApiKey", "");
        systemPrompt = cfg.getString("systemPrompt",
                "You are a helpful Minecraft assistant. Keep responses concise and relevant to Minecraft.");
        enableLogging = cfg.getBoolean("enableLogging", false);
        maxReplyLength = cfg.getInt("maxReplyLength", 500);

        // Resolve API key
        if ("ENV_OR_CONFIG".equalsIgnoreCase(apiKeySource)) {
            String envKey = System.getenv("GEMINI_API_KEY");
            if (envKey != null && !envKey.isBlank()) {
                geminiApiKey = envKey;
            }
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
    public String getGeminiApiKey() { return geminiApiKey; }
    public String getSystemPrompt() { return systemPrompt; }
    public boolean isEnableLogging() { return enableLogging; }
    public int getMaxReplyLength() { return maxReplyLength; }
}
