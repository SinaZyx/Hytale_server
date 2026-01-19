package com.kingc.hytale.factions.api.event;

import com.kingc.hytale.factions.api.Location;
import com.kingc.hytale.factions.model.Faction;

import java.util.UUID;

public record FactionHomeSetEvent(Faction faction, Location home, UUID actorId) implements FactionEvent {
}
