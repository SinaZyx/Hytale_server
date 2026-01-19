package com.kingc.hytale.factions.hytale;

import com.hypixel.hytale.server.core.Message;

/**
 * Utility class for creating formatted faction messages.
 */
public final class FactionMessages {
    // Colors
    private static final String COLOR_PREFIX = "#FFB347";      // Orange/Gold for prefix
    private static final String COLOR_ARROW = "#7a8288";       // Gray for separator
    private static final String COLOR_SUCCESS = "#7CFC00";     // Green for success
    private static final String COLOR_ERROR = "#FF6B6B";       // Red for errors
    private static final String COLOR_INFO = "#87CEEB";        // Light blue for info
    private static final String COLOR_WARNING = "#FFD700";     // Gold for warnings
    private static final String COLOR_DEFAULT = "#E8E8E8";     // Light gray for default text
    private static final String COLOR_HIGHLIGHT = "#FFFFFF";   // White for highlights
    private static final String COLOR_MUTED = "#888888";       // Dark gray for muted text

    private FactionMessages() {}

    /**
     * Creates a standard faction message with prefix.
     */
    public static Message format(String text) {
        return Message.join(
            Message.raw("⚔ ").color(COLOR_PREFIX),
            Message.raw("Factions").color(COLOR_PREFIX).bold(true),
            Message.raw(" » ").color(COLOR_ARROW),
            Message.raw(text).color(COLOR_DEFAULT)
        );
    }

    /**
     * Creates a success message (green).
     */
    public static Message success(String text) {
        return Message.join(
            Message.raw("⚔ ").color(COLOR_PREFIX),
            Message.raw("Factions").color(COLOR_PREFIX).bold(true),
            Message.raw(" » ").color(COLOR_ARROW),
            Message.raw("✓ ").color(COLOR_SUCCESS),
            Message.raw(text).color(COLOR_SUCCESS)
        );
    }

    /**
     * Creates an error message (red).
     */
    public static Message error(String text) {
        return Message.join(
            Message.raw("⚔ ").color(COLOR_PREFIX),
            Message.raw("Factions").color(COLOR_PREFIX).bold(true),
            Message.raw(" » ").color(COLOR_ARROW),
            Message.raw("✗ ").color(COLOR_ERROR),
            Message.raw(text).color(COLOR_ERROR)
        );
    }

    /**
     * Creates an info message (light blue).
     */
    public static Message info(String text) {
        return Message.join(
            Message.raw("⚔ ").color(COLOR_PREFIX),
            Message.raw("Factions").color(COLOR_PREFIX).bold(true),
            Message.raw(" » ").color(COLOR_ARROW),
            Message.raw("ℹ ").color(COLOR_INFO),
            Message.raw(text).color(COLOR_INFO)
        );
    }

    /**
     * Creates a warning message (gold).
     */
    public static Message warning(String text) {
        return Message.join(
            Message.raw("⚔ ").color(COLOR_PREFIX),
            Message.raw("Factions").color(COLOR_PREFIX).bold(true),
            Message.raw(" » ").color(COLOR_ARROW),
            Message.raw("⚠ ").color(COLOR_WARNING),
            Message.raw(text).color(COLOR_WARNING)
        );
    }

    /**
     * Creates a message with highlighted text portions.
     * Use {text} to highlight portions.
     */
    public static Message formatWithHighlight(String template, String... highlights) {
        if (highlights.length == 0) {
            return format(template);
        }

        Message prefix = Message.join(
            Message.raw("⚔ ").color(COLOR_PREFIX),
            Message.raw("Factions").color(COLOR_PREFIX).bold(true),
            Message.raw(" » ").color(COLOR_ARROW)
        );

        // Simple replacement - replace {0}, {1}, etc. with highlighted text
        String[] parts = template.split("\\{\\d+\\}");
        Message result = prefix;

        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result = Message.join(result, Message.raw(parts[i]).color(COLOR_DEFAULT));
            }
            if (i < highlights.length) {
                result = Message.join(result, Message.raw(highlights[i]).color(COLOR_HIGHLIGHT).bold(true));
            }
        }

        return result;
    }

    /**
     * Creates a separator line.
     */
    public static Message separator() {
        return Message.raw("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(COLOR_MUTED);
    }

    /**
     * Creates a header for lists/info display.
     */
    public static Message header(String title) {
        return Message.join(
            Message.raw("━━━ ").color(COLOR_MUTED),
            Message.raw("⚔ ").color(COLOR_PREFIX),
            Message.raw(title).color(COLOR_PREFIX).bold(true),
            Message.raw(" ━━━").color(COLOR_MUTED)
        );
    }

    /**
     * Creates a list item.
     */
    public static Message listItem(String text) {
        return Message.join(
            Message.raw("  • ").color(COLOR_MUTED),
            Message.raw(text).color(COLOR_DEFAULT)
        );
    }

    /**
     * Creates a list item with a label and value.
     */
    public static Message listItem(String label, String value) {
        return Message.join(
            Message.raw("  • ").color(COLOR_MUTED),
            Message.raw(label + ": ").color(COLOR_INFO),
            Message.raw(value).color(COLOR_HIGHLIGHT)
        );
    }

    /**
     * Parses a raw message string and converts [Factions] prefix to formatted version.
     * This is for backward compatibility with existing code.
     */
    public static Message parseAndFormat(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            return Message.raw("");
        }

        // Remove the old prefix if present
        String cleanMessage = rawMessage;
        if (cleanMessage.startsWith("[Factions] ")) {
            cleanMessage = cleanMessage.substring(11);
        }

        // Detect message type based on content
        String lower = cleanMessage.toLowerCase();
        if (lower.contains("error") || lower.contains("cannot") || lower.contains("not ")
            || lower.contains("invalid") || lower.contains("failed") || lower.contains("denied")
            || lower.contains("already") || lower.contains("must ")) {
            return error(cleanMessage);
        } else if (lower.contains("success") || lower.contains("created") || lower.contains("joined")
            || lower.contains("accepted") || lower.contains("set ") || lower.contains("claimed")) {
            return success(cleanMessage);
        } else if (lower.contains("warning") || lower.contains("attention") || lower.contains("guerre")
            || lower.contains("war")) {
            return warning(cleanMessage);
        }

        return format(cleanMessage);
    }
}
