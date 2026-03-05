package com.karlmendoo.mcgpt;

import com.karlmendoo.mcgpt.command.AiCommand;
import com.karlmendoo.mcgpt.config.ConfigManager;
import com.karlmendoo.mcgpt.cooldown.CooldownManager;
import com.karlmendoo.mcgpt.listener.ChatListener;
import com.karlmendoo.mcgpt.openai.OpenAiClient;
import org.bukkit.plugin.java.JavaPlugin;

public final class McgptPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private OpenAiClient openAiClient;
    private CooldownManager cooldownManager;
    private ChatListener chatListener;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.load();

        String apiKey = configManager.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            getLogger().severe("No OpenAI API key configured! Set the OPENAI_API_KEY environment variable");
            getLogger().severe("or set 'openaiApiKey' in config.yml. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        openAiClient = new OpenAiClient(this);
        cooldownManager = new CooldownManager();

        chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(chatListener, this);

        AiCommand aiCommand = new AiCommand(this);
        getCommand("ai").setExecutor(aiCommand);
        getCommand("ai").setTabCompleter(aiCommand);

        getLogger().info("McGPT enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (openAiClient != null) {
            openAiClient.shutdown();
        }
        getLogger().info("McGPT disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public OpenAiClient getOpenAiClient() {
        return openAiClient;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }
}
