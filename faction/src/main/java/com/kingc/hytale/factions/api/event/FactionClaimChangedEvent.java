package com.kingc.hytale.factions.api.event;

import com.kingc.hytale.factions.api.ClaimChangeType;
import com.kingc.hytale.factions.model.ClaimKey;
import com.kingc.hytale.factions.model.Faction;

import java.util.UUID;

public record FactionClaimChangedEvent(Faction faction, ClaimKey claim, ClaimChangeType changeType, UUID actorId)
        implements FactionEvent {
}
