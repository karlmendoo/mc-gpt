package com.karlmendoo.mcgpt.util;

import org.bukkit.ChatColor;

public class ChatFormatter {

    private ChatFormatter() {}

    /**
     * Translates '&' color codes to '§' color codes.
     */
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Strips all '§' and '&' color codes from a string.
     */
    public static String stripColors(String text) {
        if (text == null) return "";
        // Strip § color codes (Bukkit/Minecraft format)
        String stripped = ChatColor.stripColor(text);
        if (stripped == null) stripped = text;
        // Also strip any remaining & color code sequences
        stripped = stripped.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
        return stripped;
    }

    /**
     * Sanitizes player input before sending to OpenAI.
     * Removes color codes and trims whitespace.
     */
    public static String sanitizeInput(String text) {
        if (text == null) return "";
        String result = stripColors(text);
        // Remove any newlines/carriage returns that could cause issues
        result = result.replace("\n", " ").replace("\r", " ");
        return result.trim();
    }

    /**
     * Sanitizes the AI reply: strips existing color codes, replaces newlines,
     * and truncates to maxLength.
     */
    public static String sanitizeReply(String reply, int maxLength) {
        if (reply == null) return "";
        // Replace newlines with spaces to avoid chat flooding
        String result = reply.replace("\n", " ").replace("\r", " ");
        // Strip any color codes that might have slipped in from the AI
        result = stripColors(result);
        // Truncate
        if (result.length() > maxLength) {
            result = result.substring(0, maxLength) + "...";
        }
        return result.trim();
    }
}
