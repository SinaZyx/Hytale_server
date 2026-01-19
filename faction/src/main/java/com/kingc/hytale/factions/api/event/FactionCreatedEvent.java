package com.kingc.hytale.factions.api.event;

import com.kingc.hytale.factions.model.Faction;

import java.util.UUID;

public record FactionCreatedEvent(Faction faction, UUID actorId) implements FactionEvent {
}
