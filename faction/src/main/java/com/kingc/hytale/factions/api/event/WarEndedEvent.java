package com.kingc.hytale.factions.api.event;

import com.kingc.hytale.factions.model.Faction;
import com.kingc.hytale.factions.model.War;

public record WarEndedEvent(
        War war,
        Faction attacker,
        Faction defender,
        War.WarResult result,
        String reason
) implements FactionEvent {
}
