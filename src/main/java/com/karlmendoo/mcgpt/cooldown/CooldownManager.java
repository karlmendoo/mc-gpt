package com.karlmendoo.mcgpt.cooldown;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> pendingRequests = new ConcurrentHashMap<>();
    private volatile long globalCooldownExpiry = 0L;

    /**
     * Returns true if the player is on cooldown.
     */
    public boolean isOnCooldown(UUID playerId, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return false;
        Long expiry = playerCooldowns.get(playerId);
        if (expiry == null) return false;
        return System.currentTimeMillis() < expiry;
    }

    /**
     * Returns remaining cooldown in seconds (rounded up), or 0 if not on cooldown.
     */
    public int getRemainingCooldown(UUID playerId) {
        Long expiry = playerCooldowns.get(playerId);
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0) return 0;
        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * Sets the player's cooldown.
     */
    public void setCooldown(UUID playerId, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return;
        playerCooldowns.put(playerId, System.currentTimeMillis() + (cooldownSeconds * 1000L));
    }

    /**
     * Returns true if the global cooldown is active.
     */
    public boolean isGlobalCooldownActive(int globalCooldownSeconds) {
        if (globalCooldownSeconds <= 0) return false;
        return System.currentTimeMillis() < globalCooldownExpiry;
    }

    /**
     * Returns remaining global cooldown in seconds, or 0.
     */
    public int getRemainingGlobalCooldown() {
        long remaining = globalCooldownExpiry - System.currentTimeMillis();
        if (remaining <= 0) return 0;
        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * Sets the global cooldown.
     */
    public void setGlobalCooldown(int globalCooldownSeconds) {
        if (globalCooldownSeconds <= 0) return;
        globalCooldownExpiry = System.currentTimeMillis() + (globalCooldownSeconds * 1000L);
    }

    /**
     * Marks a player as having a pending request.
     */
    public void markPending(UUID playerId) {
        pendingRequests.put(playerId, Boolean.TRUE);
    }

    /**
     * Clears the pending state for a player.
     */
    public void clearPending(UUID playerId) {
        pendingRequests.remove(playerId);
    }

    /**
     * Returns true if the player has a pending request.
     */
    public boolean isPending(UUID playerId) {
        return pendingRequests.containsKey(playerId);
    }
}
