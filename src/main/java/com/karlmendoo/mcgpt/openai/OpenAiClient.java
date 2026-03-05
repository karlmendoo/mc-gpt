package com.karlmendoo.mcgpt.openai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.karlmendoo.mcgpt.McgptPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OpenAiClient {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private final McgptPlugin plugin;
    private final HttpClient httpClient;

    public OpenAiClient(McgptPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(plugin.getConfigManager().getTimeoutSeconds()))
                .build();
    }

    /**
     * Sends a message to the OpenAI Chat Completions API asynchronously.
     *
     * @param userMessage  the player's message
     * @param chatContext  recent chat messages for context (may be empty)
     * @return a CompletableFuture containing the AI's reply text
     */
    public CompletableFuture<String> sendMessage(String userMessage, List<String> chatContext) {
        var cfg = plugin.getConfigManager();
        String apiKey = cfg.getApiKey();
        String requestBody = buildRequestBody(userMessage, chatContext, cfg);

        if (cfg.isLogRequests()) {
            plugin.getLogger().info("[McGPT] Sending request to OpenAI: " + userMessage);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(cfg.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseResponse);
    }

    private String buildRequestBody(String userMessage, List<String> chatContext,
                                    com.karlmendoo.mcgpt.config.ConfigManager cfg) {
        JsonObject body = new JsonObject();
        body.addProperty("model", cfg.getModel());
        body.addProperty("temperature", cfg.getTemperature());
        body.addProperty("max_tokens", cfg.getMaxTokens());

        JsonArray messages = new JsonArray();

        // System prompt
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", cfg.getSystemPrompt());
        messages.add(systemMsg);

        // Chat context messages
        if (cfg.isIncludeChatContext() && !chatContext.isEmpty()) {
            for (String contextMsg : chatContext) {
                JsonObject ctxMsg = new JsonObject();
                ctxMsg.addProperty("role", "user");
                ctxMsg.addProperty("content", contextMsg);
                messages.add(ctxMsg);
            }
        }

        // User message
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        body.add("messages", messages);
        return body.toString();
    }

    private String parseResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String body = response.body();

        if (cfg().isLogRequests()) {
            plugin.getLogger().info("[McGPT] Response status: " + statusCode);
        }

        if (statusCode != 200) {
            plugin.getLogger().severe("[McGPT] OpenAI API error (HTTP " + statusCode + "): " + body);
            throw new OpenAiException("API returned HTTP " + statusCode);
        }

        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                plugin.getLogger().severe("[McGPT] No choices in OpenAI response: " + body);
                throw new OpenAiException("No choices in response");
            }
            String content = choices.get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();
            if (cfg().isLogRequests()) {
                plugin.getLogger().info("[McGPT] Response content: " + content);
            }
            return content;
        } catch (OpenAiException e) {
            throw e;
        } catch (Exception e) {
            plugin.getLogger().severe("[McGPT] Failed to parse OpenAI response: " + e.getMessage());
            plugin.getLogger().severe("[McGPT] Response body was: " + body);
            throw new OpenAiException("Failed to parse response: " + e.getMessage(), e);
        }
    }

    private com.karlmendoo.mcgpt.config.ConfigManager cfg() {
        return plugin.getConfigManager();
    }

    public void shutdown() {
        try {
            httpClient.close();
        } catch (Exception e) {
            plugin.getLogger().warning("[McGPT] Error closing HttpClient: " + e.getMessage());
        }
    }

    public static class OpenAiException extends RuntimeException {
        public OpenAiException(String message) {
            super(message);
        }

        public OpenAiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
