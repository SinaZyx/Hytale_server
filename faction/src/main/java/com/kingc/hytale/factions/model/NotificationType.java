package com.kingc.hytale.factions.model;

public enum NotificationType {
    MINOR,
    MAJOR,
    WAR,
    TERRITORY,
    ROLE,
    SYSTEM;

    public static NotificationType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toUpperCase();
        for (NotificationType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
