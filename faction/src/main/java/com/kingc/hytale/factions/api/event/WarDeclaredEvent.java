package com.kingc.hytale.factions.api.event;

import com.kingc.hytale.factions.model.Faction;
import com.kingc.hytale.factions.model.War;

import java.util.UUID;

public record WarDeclaredEvent(War war, Faction attacker, Faction defender, UUID actorId) implements FactionEvent {
}
