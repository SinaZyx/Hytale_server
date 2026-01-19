package com.kingc.hytale.factions.model;

import java.util.UUID;

/**
 * Statistiques de combat d'un membre de faction.
 */
public final class MemberCombatStats {
    private final UUID playerId;
    private int kills;
    private int deaths;
    private int factionKills;      // Kills contre membres d'autres factions
    private int allyKills;         // Kills contre alliés (friendly fire)
    private int enemyKills;        // Kills contre factions ennemies déclarées
    private int currentStreak;     // Série de kills actuelle
    private int bestStreak;        // Meilleure série de kills
    private UUID lastKilledBy;
    private UUID lastKilled;
    private long lastDeathTime;
    private long lastKillTime;

    public MemberCombatStats(UUID playerId) {
        this.playerId = playerId;
        this.kills = 0;
        this.deaths = 0;
        this.factionKills = 0;
        this.allyKills = 0;
        this.enemyKills = 0;
        this.currentStreak = 0;
        this.bestStreak = 0;
        this.lastDeathTime = 0;
        this.lastKillTime = 0;
    }

    public UUID playerId() {
        return playerId;
    }

    public int kills() {
        return kills;
    }

    public int deaths() {
        return deaths;
    }

    public int factionKills() {
        return factionKills;
    }

    public int allyKills() {
        return allyKills;
    }

    public int enemyKills() {
        return enemyKills;
    }

    public int currentStreak() {
        return currentStreak;
    }

    public int bestStreak() {
        return bestStreak;
    }

    public UUID lastKilledBy() {
        return lastKilledBy;
    }

    public UUID lastKilled() {
        return lastKilled;
    }

    public long lastDeathTime() {
        return lastDeathTime;
    }

    public long lastKillTime() {
        return lastKillTime;
    }

    public double kdr() {
        if (deaths == 0) {
            return kills;
        }
        return Math.round((double) kills / deaths * 100.0) / 100.0;
    }

    public void recordKill(UUID victimId, KillType type, long timestamp) {
        this.kills++;
        this.lastKilled = victimId;
        this.lastKillTime = timestamp;
        this.currentStreak++;
        if (this.currentStreak > this.bestStreak) {
            this.bestStreak = this.currentStreak;
        }

        switch (type) {
            case FACTION -> this.factionKills++;
            case ALLY -> this.allyKills++;
            case ENEMY -> this.enemyKills++;
            case NEUTRAL -> {} // Just a regular kill
        }
    }

    public void recordDeath(UUID killerId, long timestamp) {
        this.deaths++;
        this.lastKilledBy = killerId;
        this.lastDeathTime = timestamp;
        this.currentStreak = 0;
    }

    // For deserialization
    public void setStats(int kills, int deaths, int factionKills, int allyKills, int enemyKills,
                         int currentStreak, int bestStreak, UUID lastKilledBy, UUID lastKilled,
                         long lastDeathTime, long lastKillTime) {
        this.kills = kills;
        this.deaths = deaths;
        this.factionKills = factionKills;
        this.allyKills = allyKills;
        this.enemyKills = enemyKills;
        this.currentStreak = currentStreak;
        this.bestStreak = bestStreak;
        this.lastKilledBy = lastKilledBy;
        this.lastKilled = lastKilled;
        this.lastDeathTime = lastDeathTime;
        this.lastKillTime = lastKillTime;
    }

    public enum KillType {
        NEUTRAL,    // Kill against player without faction or neutral faction
        FACTION,    // Kill against another faction member
        ALLY,       // Kill against allied faction (friendly fire)
        ENEMY       // Kill against declared enemy faction
    }
}
