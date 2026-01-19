package com.kingc.hytale.factions.api;

import com.kingc.hytale.factions.model.MemberRole;

import java.util.UUID;

public interface MemberRoleChangeHandler {
    void handle(UUID actorId, UUID targetId, MemberRole newRole, boolean promoted);
}
