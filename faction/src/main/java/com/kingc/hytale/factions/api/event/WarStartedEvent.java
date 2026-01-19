package com.kingc.hytale.factions.api.event;

import com.kingc.hytale.factions.model.Faction;
import com.kingc.hytale.factions.model.War;

public record WarStartedEvent(War war, Faction attacker, Faction defender) implements FactionEvent {
}
