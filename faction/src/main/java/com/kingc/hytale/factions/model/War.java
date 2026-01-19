package com.kingc.hytale.factions.model;

import java.util.UUID;

/**
 * Représente une guerre entre deux factions.
 */
public final class War {
    private final UUID id;
    private final UUID attackerFactionId;
    private final UUID defenderFactionId;
    private final long startTime;
    private final long gracePeriodEnd;  // Période de grâce avant le début des hostilités

    private WarState state;
    private int attackerPoints;
    private int defenderPoints;
    private int attackerKills;
    private int defenderKills;
    private long endTime;
    private WarResult result;
    private String endReason;

    public War(UUID id, UUID attackerFactionId, UUID defenderFactionId, long startTime, long gracePeriodEnd) {
        this.id = id;
        this.attackerFactionId = attackerFactionId;
        this.defenderFactionId = defenderFactionId;
        this.startTime = startTime;
        this.gracePeriodEnd = gracePeriodEnd;
        this.state = WarState.PENDING;
        this.attackerPoints = 0;
        this.defenderPoints = 0;
        this.attackerKills = 0;
        this.defenderKills = 0;
        this.endTime = 0;
        this.result = null;
        this.endReason = null;
    }

    public UUID id() {
        return id;
    }

    public UUID attackerFactionId() {
        return attackerFactionId;
    }

    public UUID defenderFactionId() {
        return defenderFactionId;
    }

    public long startTime() {
        return startTime;
    }

    public long gracePeriodEnd() {
        return gracePeriodEnd;
    }

    public WarState state() {
        return state;
    }

    public void setState(WarState state) {
        this.state = state;
    }

    public int attackerPoints() {
        return attackerPoints;
    }

    public int defenderPoints() {
        return defenderPoints;
    }

    public int attackerKills() {
        return attackerKills;
    }

    public int defenderKills() {
        return defenderKills;
    }

    public long endTime() {
        return endTime;
    }

    public WarResult result() {
        return result;
    }

    public String endReason() {
        return endReason;
    }

    /**
     * Enregistre un kill pendant la guerre.
     * @param killerFactionId La faction du tueur
     * @param pointsPerKill Points gagnés par kill
     * @return true si le kill a été enregistré
     */
    public boolean recordKill(UUID killerFactionId, int pointsPerKill) {
        if (state != WarState.ACTIVE) {
            return false;
        }
        if (killerFactionId.equals(attackerFactionId)) {
            attackerKills++;
            attackerPoints += pointsPerKill;
            return true;
        } else if (killerFactionId.equals(defenderFactionId)) {
            defenderKills++;
            defenderPoints += pointsPerKill;
            return true;
        }
        return false;
    }

    /**
     * Vérifie si un joueur d'une faction peut gagner des points de guerre.
     */
    public boolean isParticipant(UUID factionId) {
        return factionId.equals(attackerFactionId) || factionId.equals(defenderFactionId);
    }

    /**
     * Termine la guerre.
     */
    public void end(WarResult result, String reason, long endTime) {
        this.state = WarState.ENDED;
        this.result = result;
        this.endReason = reason;
        this.endTime = endTime;
    }

    /**
     * Active la guerre (après la période de grâce).
     */
    public void activate() {
        if (this.state == WarState.PENDING) {
            this.state = WarState.ACTIVE;
        }
    }

    /**
     * Calcule le gagnant actuel basé sur les points.
     */
    public UUID currentLeader() {
        if (attackerPoints > defenderPoints) {
            return attackerFactionId;
        } else if (defenderPoints > attackerPoints) {
            return defenderFactionId;
        }
        return null; // Égalité
    }

    // Pour la désérialisation
    public void setStats(int attackerPoints, int defenderPoints, int attackerKills, int defenderKills,
                         WarState state, WarResult result, String endReason, long endTime) {
        this.attackerPoints = attackerPoints;
        this.defenderPoints = defenderPoints;
        this.attackerKills = attackerKills;
        this.defenderKills = defenderKills;
        this.state = state;
        this.result = result;
        this.endReason = endReason;
        this.endTime = endTime;
    }

    public enum WarState {
        PENDING,    // Période de grâce, la guerre n'a pas encore commencé
        ACTIVE,     // Guerre en cours
        ENDED       // Guerre terminée
    }

    public enum WarResult {
        ATTACKER_WIN,   // L'attaquant a gagné
        DEFENDER_WIN,   // Le défenseur a gagné
        DRAW,           // Égalité
        SURRENDER,      // Reddition d'une faction
        CANCELLED       // Guerre annulée (admin)
    }
}
