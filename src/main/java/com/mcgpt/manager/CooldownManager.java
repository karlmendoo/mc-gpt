package com.mcgpt.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> pendingRequests = new ConcurrentHashMap<>();
    private volatile long globalCooldownUntil = 0L;

    /**
     * Checks if a player is on cooldown.
     *
     * @param uuid             the player UUID
     * @param cooldownSeconds  per-player cooldown in seconds (0 = disabled)
     * @return remaining seconds, or 0 if not on cooldown
     */
    public int getPlayerCooldownRemaining(UUID uuid, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return 0;
        Long lastUse = playerCooldowns.get(uuid);
        if (lastUse == null) return 0;
        long elapsed = (System.currentTimeMillis() - lastUse) / 1000L;
        int remaining = (int) (cooldownSeconds - elapsed);
        return Math.max(remaining, 0);
    }

    /**
     * Checks if the global cooldown is active.
     *
     * @return remaining seconds, or 0 if not active
     */
    public int getGlobalCooldownRemaining() {
        long now = System.currentTimeMillis();
        if (now >= globalCooldownUntil) return 0;
        return (int) ((globalCooldownUntil - now) / 1000L);
    }

    /**
     * Records a player's use and resets their cooldown.
     */
    public void recordUse(UUID uuid, int cooldownSeconds, int globalCooldownSeconds) {
        if (cooldownSeconds > 0) {
            playerCooldowns.put(uuid, System.currentTimeMillis());
        }
        if (globalCooldownSeconds > 0) {
            globalCooldownUntil = System.currentTimeMillis() + (globalCooldownSeconds * 1000L);
        }
    }

    /**
     * Marks a player as having a pending request.
     *
     * @return false if a request is already pending
     */
    public boolean tryMarkPending(UUID uuid) {
        return pendingRequests.putIfAbsent(uuid, Boolean.TRUE) == null;
    }

    /**
     * Clears the pending state for a player.
     */
    public void clearPending(UUID uuid) {
        pendingRequests.remove(uuid);
    }

    /**
     * Returns whether a player has a pending request.
     */
    public boolean isPending(UUID uuid) {
        return pendingRequests.containsKey(uuid);
    }
}
