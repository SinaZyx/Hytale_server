package com.kingc.hytale.duels.queue;

import com.kingc.hytale.duels.match.MatchType;

import java.util.UUID;

public record QueueEntry(
    UUID playerId,
    MatchType type,
    String kitId,
    long joinedAt
) {}
