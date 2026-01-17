package com.kingc.hytale.factions.model;

import com.kingc.hytale.factions.api.Location;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Faction {
    private final UUID id;
    private final long createdAtEpochMs;
    private final Map<UUID, MemberRole> members;
    private final Set<UUID> allies;

    private String name;
    private String description;
    private Location home;
    private Integer powerOverride;

    public Faction(UUID id, String name, long createdAtEpochMs, Map<UUID, MemberRole> members) {
        this(id, name, createdAtEpochMs, members, Set.of());
    }

    public Faction(UUID id, String name, long createdAtEpochMs, Map<UUID, MemberRole> members, Set<UUID> allies) {
        this.id = id;
        this.name = name;
        this.createdAtEpochMs = createdAtEpochMs;
        this.members = new HashMap<>(members);
        this.allies = new HashSet<>(allies);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long createdAtEpochMs() {
        return createdAtEpochMs;
    }

    public Location home() {
        return home;
    }

    public void setHome(Location home) {
        this.home = home;
    }

    public Integer powerOverride() {
        return powerOverride;
    }

    public void setPowerOverride(Integer powerOverride) {
        this.powerOverride = powerOverride;
    }

    public Map<UUID, MemberRole> members() {
        return Collections.unmodifiableMap(members);
    }

    public Set<UUID> allies() {
        return Collections.unmodifiableSet(allies);
    }

    public MemberRole roleOf(UUID playerId) {
        return members.get(playerId);
    }

    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public void setMemberRole(UUID playerId, MemberRole role) {
        members.put(playerId, role);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public void addAlly(UUID factionId) {
        allies.add(factionId);
    }

    public void removeAlly(UUID factionId) {
        allies.remove(factionId);
    }
}
