package com.kingc.hytale.factions.model;

import java.util.UUID;

public final class FactionInvite {
    private final UUID factionId;
    private final UUID inviterId;
    private final long expiresAtEpochMs;

    public FactionInvite(UUID factionId, UUID inviterId, long expiresAtEpochMs) {
        this.factionId = factionId;
        this.inviterId = inviterId;
        this.expiresAtEpochMs = expiresAtEpochMs;
    }

    public UUID factionId() {
        return factionId;
    }

    public UUID inviterId() {
        return inviterId;
    }

    public long expiresAtEpochMs() {
        return expiresAtEpochMs;
    }

    public boolean isExpired(long nowEpochMs) {
        return nowEpochMs > expiresAtEpochMs;
    }
}
