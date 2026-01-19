package com.kingc.hytale.factions.api;

import com.kingc.hytale.factions.model.War;

import java.util.UUID;

public interface WarDeclareHandler {
    void handle(UUID actorId, War war);
}
