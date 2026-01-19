package com.kingc.hytale.factions.model;

public final class NotificationEntry {
    private final NotificationType type;
    private final String title;
    private final String message;
    private final long timestamp;

    public NotificationEntry(NotificationType type, String title, String message, long timestamp) {
        this.type = type == null ? NotificationType.SYSTEM : type;
        this.title = title == null ? "" : title;
        this.message = message == null ? "" : message;
        this.timestamp = timestamp;
    }

    public NotificationType type() {
        return type;
    }

    public String title() {
        return title;
    }

    public String message() {
        return message;
    }

    public long timestamp() {
        return timestamp;
    }
}
