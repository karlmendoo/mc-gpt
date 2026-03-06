package com.mcgpt.manager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores per-player AI conversation history as user/model exchange pairs,
 * so that subsequent requests can include prior context.
 */
public class AiConversationManager {

    private final Map<UUID, Deque<String[]>> playerConversations = new ConcurrentHashMap<>();
    private final int maxExchanges;

    public AiConversationManager(int maxExchanges) {
        if (maxExchanges <= 0) {
            throw new IllegalArgumentException("maxExchanges must be a positive integer, got: " + maxExchanges);
        }
        this.maxExchanges = maxExchanges;
    }

    /**
     * Records an AI exchange for a player. Each exchange is a [userMessage, aiReply] pair.
     */
    public void addExchange(UUID uuid, String userMessage, String aiReply) {
        playerConversations.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        Deque<String[]> history = playerConversations.get(uuid);
        synchronized (history) {
            if (history.size() >= maxExchanges) {
                history.pollFirst();
            }
            history.addLast(new String[]{userMessage, aiReply});
        }
    }

    /**
     * Returns the conversation history for a player as a list of [userMessage, aiReply] pairs,
     * oldest first.
     */
    public List<String[]> getHistory(UUID uuid) {
        Deque<String[]> history = playerConversations.get(uuid);
        if (history == null || history.isEmpty()) return List.of();
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    /**
     * Clears the AI conversation history for a player.
     */
    public void clearHistory(UUID uuid) {
        playerConversations.remove(uuid);
    }
}
