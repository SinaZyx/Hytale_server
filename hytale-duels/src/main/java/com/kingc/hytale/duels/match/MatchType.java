package com.kingc.hytale.duels.match;

public enum MatchType {
    DUEL_1V1(1),
    DUEL_2V2(2);

    private final int teamSize;

    MatchType(int teamSize) {
        this.teamSize = teamSize;
    }

    public int teamSize() {
        return teamSize;
    }

    public int totalPlayers() {
        return teamSize * 2;
    }
}
