package com.kingc.hytale.factions.api.event;

import com.kingc.hytale.factions.model.Faction;

import java.util.UUID;

public record FactionTreasuryChangedEvent(
        Faction faction,
        UUID actorId,
        double amount,
        double oldBalance,
        double newBalance
) implements FactionEvent {
}
