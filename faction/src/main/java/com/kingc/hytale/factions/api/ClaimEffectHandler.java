package com.kingc.hytale.factions.api;

import java.util.Optional;
import java.util.UUID;

public interface ClaimEffectHandler {
    void handle(UUID actorId, Location location, ClaimChangeType type, Optional<UUID> ownerId);
}
