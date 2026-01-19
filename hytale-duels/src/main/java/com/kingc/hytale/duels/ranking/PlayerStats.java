package com.kingc.hytale.duels.ranking;

import java.util.UUID;

public final class PlayerStats {
    public static final int DEFAULT_ELO = 1000;
    public static final int MIN_ELO = 100;
    public static final int MAX_ELO = 3000;

    private final UUID playerId;
    private String playerName;
    private int elo;
    private int wins;
    private int losses;
    private int winStreak;
    private int bestWinStreak;
    private long lastMatchTime;

    public PlayerStats(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.elo = DEFAULT_ELO;
        this.wins = 0;
        this.losses = 0;
        this.winStreak = 0;
        this.bestWinStreak = 0;
        this.lastMatchTime = 0;
    }

    // Pour la deserialisation JSON
    public PlayerStats() {
        this.playerId = null;
        this.playerName = "";
        this.elo = DEFAULT_ELO;
    }

    public UUID playerId() {
        return playerId;
    }

    public String playerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int elo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = Math.max(MIN_ELO, Math.min(MAX_ELO, elo));
    }

    public void addElo(int amount) {
        setElo(this.elo + amount);
    }

    public int wins() {
        return wins;
    }

    public int losses() {
        return losses;
    }

    public int totalMatches() {
        return wins + losses;
    }

    public double winRate() {
        if (totalMatches() == 0) {
            return 0.0;
        }
        return (double) wins / totalMatches() * 100.0;
    }

    public int winStreak() {
        return winStreak;
    }

    public int bestWinStreak() {
        return bestWinStreak;
    }

    public long lastMatchTime() {
        return lastMatchTime;
    }

    public void recordWin(long time) {
        this.wins++;
        this.winStreak++;
        if (this.winStreak > this.bestWinStreak) {
            this.bestWinStreak = this.winStreak;
        }
        this.lastMatchTime = time;
    }

    public void recordLoss(long time) {
        this.losses++;
        this.winStreak = 0;
        this.lastMatchTime = time;
    }

    public Rank getRank() {
        return Rank.fromElo(elo);
    }

    @Override
    public String toString() {
        return playerName + " [" + getRank().displayName() + " " + elo + " ELO] " + wins + "W/" + losses + "L";
    }
}
