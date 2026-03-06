package com.mcgpt.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcgpt.config.ConfigManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class OpenAIClient {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private final ConfigManager config;
    private final Logger logger;
    private final HttpClient httpClient;

    public OpenAIClient(ConfigManager config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
    }

    /**
     * Sends a message to OpenAI asynchronously and returns the response text.
     *
     * @param userMessage the player's message
     * @param context     optional chat context (may be empty)
     * @return CompletableFuture resolving to the AI reply text
     */
    public CompletableFuture<String> ask(String userMessage, String context) {
        String apiKey = config.getOpenaiApiKey();

        JsonObject body = buildRequestBody(userMessage, context);
        String requestJson = body.toString();

        if (config.isEnableLogging()) {
            logger.info("[McGPT] Sending request to OpenAI: " + requestJson);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (config.isEnableLogging()) {
                        logger.info("[McGPT] OpenAI response status: " + response.statusCode());
                        logger.info("[McGPT] OpenAI response body: " + response.body());
                    }
                    if (response.statusCode() != 200) {
                        String errorDetail = switch (response.statusCode()) {
                            case 401 -> "Invalid or missing API key. Check your OPENAI_API_KEY.";
                            case 402 -> "Insufficient OpenAI credits. Please check your billing.";
                            case 429 -> "OpenAI rate limit exceeded. Please wait before trying again.";
                            case 500, 502, 503 -> "OpenAI service is temporarily unavailable.";
                            default -> "HTTP " + response.statusCode();
                        };
                        logger.warning("[McGPT] OpenAI API error (" + response.statusCode()
                                + "): " + errorDetail + " | Body: " + response.body());
                        throw new RuntimeException("API error: " + errorDetail);
                    }
                    return parseReply(response.body());
                });
    }

    private JsonObject buildRequestBody(String userMessage, String context) {
        JsonObject body = new JsonObject();
        body.addProperty("model", config.getModel());
        body.addProperty("temperature", config.getTemperature());
        body.addProperty("max_tokens", config.getMaxTokens());

        JsonArray messages = new JsonArray();

        // System prompt
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", config.getSystemPrompt());
        messages.add(systemMsg);

        // Context if provided
        if (context != null && !context.isBlank()) {
            JsonObject contextMsg = new JsonObject();
            contextMsg.addProperty("role", "user");
            contextMsg.addProperty("content", "Recent chat context:\n" + context);
            messages.add(contextMsg);
        }

        // User message
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        body.add("messages", messages);
        return body;
    }

    private String parseReply(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                return "No response from AI.";
            }
            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            JsonObject message = firstChoice.getAsJsonObject("message");
            return message.get("content").getAsString();
        } catch (Exception e) {
            logger.warning("[McGPT] Failed to parse OpenAI response: " + e.getMessage());
            return "Error parsing AI response.";
        }
    }
}
