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
        JsonArray systemParts = new JsonArray();
        JsonObject systemPart = new JsonObject();
        systemPart.addProperty("text", config.getSystemPrompt());
        systemParts.add(systemPart);
        systemInstruction.add("parts", systemParts);
        body.add("systemInstruction", systemInstruction);

        // Contents array
        JsonArray contents = new JsonArray();

        // Context if provided
        if (context != null && !context.isBlank()) {
            JsonObject contextMsg = new JsonObject();
            contextMsg.addProperty("role", "user");
            JsonArray contextParts = new JsonArray();
            JsonObject contextPart = new JsonObject();
            contextPart.addProperty("text", "Recent chat context:\n" + context);
            contextParts.add(contextPart);
            contextMsg.add("parts", contextParts);
            contents.add(contextMsg);

            // Placeholder model response to maintain valid alternating turn structure
            JsonObject contextAck = new JsonObject();
            contextAck.addProperty("role", "model");
            JsonArray ackParts = new JsonArray();
            JsonObject ackPart = new JsonObject();
            ackPart.addProperty("text", CONTEXT_ACK_TEXT);
            ackParts.add(ackPart);
            contextAck.add("parts", ackParts);
            contents.add(contextAck);
        }

        // User message
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        JsonArray userParts = new JsonArray();
        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", userMessage);
        userParts.add(userPart);
        userMsg.add("parts", userParts);
        contents.add(userMsg);

        body.add("contents", contents);

        // Generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", config.getTemperature());
        generationConfig.addProperty("maxOutputTokens", config.getMaxTokens());
        body.add("generationConfig", generationConfig);

        return body;
    }

    private String parseReply(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return "No response from AI.";
            }
            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
            JsonObject content = firstCandidate.getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");
            return parts.get(0).getAsJsonObject().get("text").getAsString();
        } catch (Exception e) {
            logger.warning("[McGPT] Failed to parse Gemini response: " + e.getMessage());
            return "Error parsing AI response.";
        }
    }
}
