package com.mcgpt.manager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatContextManager {

    private final Map<UUID, Deque<String>> playerContexts = new ConcurrentHashMap<>();
    private final int maxMessages;

    public ChatContextManager(int maxMessages) {
        if (maxMessages <= 0) {
            throw new IllegalArgumentException("maxMessages must be a positive integer, got: " + maxMessages);
        }
        this.maxMessages = maxMessages;
    }

    /**
     * Records a chat message for a player.
     */
    public void addMessage(UUID uuid, String message) {
        playerContexts.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        Deque<String> history = playerContexts.get(uuid);
        synchronized (history) {
            if (history.size() >= maxMessages) {
                history.pollFirst();
            }
            history.addLast(message);
        }
    }

    /**
     * Returns the recent chat history for a player as a formatted string.
     */
    public String getContext(UUID uuid) {
        Deque<String> history = playerContexts.get(uuid);
        if (history == null || history.isEmpty()) return "";
        synchronized (history) {
            StringBuilder sb = new StringBuilder();
            for (String msg : history) {
                sb.append(msg).append("\n");
            }
            return sb.toString().trim();
        }
    }

    /**
     * Clears chat history for a player.
     */
    public void clearContext(UUID uuid) {
        playerContexts.remove(uuid);
    }
}
