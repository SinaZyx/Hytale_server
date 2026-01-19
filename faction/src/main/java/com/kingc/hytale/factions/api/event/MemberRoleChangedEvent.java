package com.kingc.hytale.factions.api.event;

import com.kingc.hytale.factions.model.Faction;
import com.kingc.hytale.factions.model.MemberRole;

import java.util.UUID;

public record MemberRoleChangedEvent(
        Faction faction,
        UUID actorId,
        UUID targetId,
        MemberRole oldRole,
        MemberRole newRole,
        MemberRoleChangeType changeType
) implements FactionEvent {
}
