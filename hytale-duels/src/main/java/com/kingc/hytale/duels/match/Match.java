package com.kingc.hytale.duels.match;

import java.util.List;
import java.util.UUID;

public final class Match {
    private final String id;
    private final String arenaId;
    private final String kitId;
    private final MatchType type;
    private final List<UUID> team1;
    private final List<UUID> team2;
    private final long createdAt;
    private MatchState state;
    private long startedAt;
    private long endedAt;
    private List<UUID> winners;

    public Match(String id, String arenaId, String kitId, MatchType type, List<UUID> team1, List<UUID> team2, long createdAt) {
        this.id = id;
        this.arenaId = arenaId;
        this.kitId = kitId;
        this.type = type;
        this.team1 = List.copyOf(team1);
        this.team2 = List.copyOf(team2);
        this.createdAt = createdAt;
        this.state = MatchState.WAITING;
    }

    public String id() {
        return id;
    }

    public String arenaId() {
        return arenaId;
    }

    public String kitId() {
        return kitId;
    }

    public MatchType type() {
        return type;
    }

    public List<UUID> team1() {
        return team1;
    }

    public List<UUID> team2() {
        return team2;
    }

    public List<UUID> allPlayers() {
        return java.util.stream.Stream.concat(team1.stream(), team2.stream()).toList();
    }

    public long createdAt() {
        return createdAt;
    }

    public MatchState state() {
        return state;
    }

    public void start(long now) {
        this.state = MatchState.RUNNING;
        this.startedAt = now;
    }

    public void end(List<UUID> winners, long now) {
        this.state = MatchState.FINISHED;
        this.winners = List.copyOf(winners);
        this.endedAt = now;
    }

    public List<UUID> winners() {
        return winners;
    }

    public boolean isPlayerInMatch(UUID playerId) {
        return team1.contains(playerId) || team2.contains(playerId);
    }

    public int getTeamOf(UUID playerId) {
        if (team1.contains(playerId)) return 1;
        if (team2.contains(playerId)) return 2;
        return 0;
    }
}
