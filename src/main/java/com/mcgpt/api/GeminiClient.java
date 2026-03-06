package com.mcgpt.api;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Part;
import com.mcgpt.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class GeminiClient {

    private static final String CONTEXT_ACK_TEXT = "Understood.";

    private final ConfigManager config;
    private final Logger logger;
    private final Client client;

    public GeminiClient(ConfigManager config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.client = Client.builder()
                .apiKey(config.getGeminiApiKey())
                .httpOptions(HttpOptions.builder().timeout(config.getTimeoutSeconds() * 1000).build())
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
        GenerateContentConfig genConfig = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(config.getSystemPrompt())))
                .temperature((float) config.getTemperature())
                .maxOutputTokens(config.getMaxTokens())
                .build();

        if (config.isEnableLogging()) {
            logger.info("[McGPT] Sending request to Gemini model: " + config.getModel());
        }

        CompletableFuture<GenerateContentResponse> responseFuture;

        if (context != null && !context.isBlank()) {
            // Multi-turn: include context as preceding conversation turns
            List<Content> contents = new ArrayList<>();
            contents.add(Content.builder()
                    .role("user")
                    .parts(Part.fromText("Recent chat context:\n" + context))
                    .build());
            contents.add(Content.builder()
                    .role("model")
                    .parts(Part.fromText(CONTEXT_ACK_TEXT))
                    .build());
            contents.add(Content.fromParts(Part.fromText(userMessage)));
            responseFuture = client.async.models.generateContent(config.getModel(), contents, genConfig);
        } else {
            responseFuture = client.async.models.generateContent(config.getModel(), userMessage, genConfig);
        }

        return responseFuture.thenApply(response -> {
            if (config.isEnableLogging()) {
                logger.info("[McGPT] Gemini response received.");
            }
            String text = response.text();
            if (text == null || text.isBlank()) {
                return "No response from AI.";
            }
            return text;
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            logger.warning("[McGPT] Gemini API error: " + cause.getMessage());
            throw new RuntimeException("Gemini API error: " + cause.getMessage(), cause);
        });
    }
}
