package com.kingc.hytale.factions.model;

import java.util.UUID;

/**
 * Statistiques de combat globales d'une faction.
 */
public final class FactionCombatStats {
    private final UUID factionId;
    private int totalKills;
    private int totalDeaths;
    private int warsWon;
    private int warsLost;
    private int warsDraw;
    private int territoriesCaptured;
    private int territoriesLost;

    public FactionCombatStats(UUID factionId) {
        this.factionId = factionId;
        this.totalKills = 0;
        this.totalDeaths = 0;
        this.warsWon = 0;
        this.warsLost = 0;
        this.warsDraw = 0;
        this.territoriesCaptured = 0;
        this.territoriesLost = 0;
    }

    public UUID factionId() {
        return factionId;
    }

    public int totalKills() {
        return totalKills;
    }

    public int totalDeaths() {
        return totalDeaths;
    }

    public int warsWon() {
        return warsWon;
    }

    public int warsLost() {
        return warsLost;
    }

    public int warsDraw() {
        return warsDraw;
    }

    public int totalWars() {
        return warsWon + warsLost + warsDraw;
    }

    public int territoriesCaptured() {
        return territoriesCaptured;
    }

    public int territoriesLost() {
        return territoriesLost;
    }

    public double kdr() {
        if (totalDeaths == 0) {
            return totalKills;
        }
        return Math.round((double) totalKills / totalDeaths * 100.0) / 100.0;
    }

    public double winRate() {
        int total = totalWars();
        if (total == 0) {
            return 0;
        }
        return Math.round((double) warsWon / total * 100.0) / 100.0;
    }

    public void recordKill() {
        this.totalKills++;
    }

    public void recordDeath() {
        this.totalDeaths++;
    }

    public void recordWarWin() {
        this.warsWon++;
    }

    public void recordWarLoss() {
        this.warsLost++;
    }

    public void recordWarDraw() {
        this.warsDraw++;
    }

    public void recordTerritoryCaptured() {
        this.territoriesCaptured++;
    }

    public void recordTerritoryLost() {
        this.territoriesLost++;
    }

    // Pour la désérialisation
    public void setStats(int totalKills, int totalDeaths, int warsWon, int warsLost, int warsDraw,
                         int territoriesCaptured, int territoriesLost) {
        this.totalKills = totalKills;
        this.totalDeaths = totalDeaths;
        this.warsWon = warsWon;
        this.warsLost = warsLost;
        this.warsDraw = warsDraw;
        this.territoriesCaptured = territoriesCaptured;
        this.territoriesLost = territoriesLost;
    }
}
