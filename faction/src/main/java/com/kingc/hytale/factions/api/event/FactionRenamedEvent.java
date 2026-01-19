package com.kingc.hytale.factions.api.event;

import com.kingc.hytale.factions.model.Faction;

import java.util.UUID;

public record FactionRenamedEvent(Faction faction, String oldName, UUID actorId) implements FactionEvent {
}
