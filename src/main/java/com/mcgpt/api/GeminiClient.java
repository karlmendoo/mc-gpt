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

public class GeminiClient {

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String CONTEXT_ACK_TEXT = "Understood.";

    private final ConfigManager config;
    private final Logger logger;
    private final HttpClient httpClient;

    public GeminiClient(ConfigManager config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
    }

    /**
     * Sends a message to Gemini asynchronously and returns the response text.
     *
     * @param userMessage the player's message
     * @param context     optional chat context (may be empty)
     * @return CompletableFuture resolving to the AI reply text
     */
    public CompletableFuture<String> ask(String userMessage, String context) {
        String apiKey = config.getGeminiApiKey();
        String url = API_URL + config.getModel() + ":generateContent?key=" + apiKey;

        JsonObject body = buildRequestBody(userMessage, context);
        String requestJson = body.toString();

        if (config.isEnableLogging()) {
            logger.info("[McGPT] Sending request to Gemini: " + requestJson);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (config.isEnableLogging()) {
                        logger.info("[McGPT] Gemini response status: " + response.statusCode());
                        logger.info("[McGPT] Gemini response body: " + response.body());
                    }
                    if (response.statusCode() != 200) {
                        String errorDetail = switch (response.statusCode()) {
                            case 400 -> "Bad request. Check your model name or request format.";
                            case 401, 403 -> "Invalid or missing API key. Check your GEMINI_API_KEY.";
                            case 429 -> "Gemini rate limit exceeded. Please wait before trying again.";
                            case 500, 502, 503 -> "Gemini service is temporarily unavailable.";
                            default -> "HTTP " + response.statusCode();
                        };
                        logger.warning("[McGPT] Gemini API error (" + response.statusCode()
                                + "): " + errorDetail + " | Body: " + response.body());
                        throw new RuntimeException("API error: " + errorDetail);
                    }
                    return parseReply(response.body());
                });
    }

    private JsonObject buildRequestBody(String userMessage, String context) {
        JsonObject body = new JsonObject();

        // System instruction
        JsonObject systemInstruction = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysPart = new JsonObject();
        sysPart.addProperty("text", config.getSystemPrompt());
        sysParts.add(sysPart);
        systemInstruction.add("parts", sysParts);
        body.add("systemInstruction", systemInstruction);

        // Contents
        JsonArray contents = new JsonArray();

        if (context != null && !context.isBlank()) {
            // Add context as a preceding user/model exchange
            JsonObject ctxUserTurn = new JsonObject();
            ctxUserTurn.addProperty("role", "user");
            JsonArray ctxUserParts = new JsonArray();
            JsonObject ctxUserPart = new JsonObject();
            ctxUserPart.addProperty("text", "Recent chat context:\n" + context);
            ctxUserParts.add(ctxUserPart);
            ctxUserTurn.add("parts", ctxUserParts);
            contents.add(ctxUserTurn);

            JsonObject ctxModelTurn = new JsonObject();
            ctxModelTurn.addProperty("role", "model");
            JsonArray ctxModelParts = new JsonArray();
            JsonObject ctxModelPart = new JsonObject();
            ctxModelPart.addProperty("text", CONTEXT_ACK_TEXT);
            ctxModelParts.add(ctxModelPart);
            ctxModelTurn.add("parts", ctxModelParts);
            contents.add(ctxModelTurn);
        }

        // Actual user message
        JsonObject userTurn = new JsonObject();
        userTurn.addProperty("role", "user");
        JsonArray userParts = new JsonArray();
        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", userMessage);
        userParts.add(userPart);
        userTurn.add("parts", userParts);
        contents.add(userTurn);

        body.add("contents", contents);

        // Generation config
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", config.getTemperature());
        genConfig.addProperty("maxOutputTokens", config.getMaxTokens());
        body.add("generationConfig", genConfig);

        return body;
    }

    private String parseReply(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return "No response from AI.";
            }
            JsonObject candidate = candidates.get(0).getAsJsonObject();
            JsonObject content = candidate.getAsJsonObject("content");
            if (content == null) {
                return "No response from AI.";
            }
            JsonArray parts = content.getAsJsonArray("parts");
            if (parts == null || parts.isEmpty()) {
                return "No response from AI.";
            }
            com.google.gson.JsonElement textElement = parts.get(0).getAsJsonObject().get("text");
            if (textElement == null || textElement.isJsonNull()) {
                return "No response from AI.";
            }
            return textElement.getAsString();
        } catch (Exception e) {
            logger.warning("[McGPT] Failed to parse Gemini response: " + e.getMessage());
            return "Error parsing AI response.";
        }
    }
}
