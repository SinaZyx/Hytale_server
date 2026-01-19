package com.kingc.hytale.duels.match;

import java.util.UUID;

public record DuelRequest(
    UUID senderId,
    UUID targetId,
    String kitId,
    long createdAt,
    long expiresAt
) {
    private static final long DEFAULT_EXPIRY_MS = 30_000;

    public static DuelRequest create(UUID senderId, UUID targetId, String kitId, long now) {
        return new DuelRequest(senderId, targetId, kitId, now, now + DEFAULT_EXPIRY_MS);
    }

    public boolean isExpired(long now) {
        return now >= expiresAt;
    }
}
