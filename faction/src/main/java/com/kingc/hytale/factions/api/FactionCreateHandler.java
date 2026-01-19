package com.kingc.hytale.factions.api;

import java.util.UUID;

public interface FactionCreateHandler {
    void handle(UUID actorId, Location location, UUID factionId, String factionName);
}
