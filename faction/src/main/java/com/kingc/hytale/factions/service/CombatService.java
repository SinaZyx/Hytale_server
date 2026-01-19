package com.kingc.hytale.factions.service;

import com.kingc.hytale.factions.model.Faction;
import com.kingc.hytale.factions.model.FactionCombatStats;
import com.kingc.hytale.factions.model.MemberCombatStats;
import com.kingc.hytale.factions.model.War;
import com.kingc.hytale.factions.api.event.FactionEventBus;
import com.kingc.hytale.factions.api.event.WarDeclaredEvent;
import com.kingc.hytale.factions.api.event.WarEndedEvent;
import com.kingc.hytale.factions.api.event.WarStartedEvent;
import com.kingc.hytale.factions.storage.CombatDataStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Service de gestion des statistiques de combat et des guerres.
 */
public final class CombatService {
    private final CombatDataStore store;
    private final FactionService factionService;
    private final TimeProvider timeProvider;
    private final ActionLogger actionLogger;
    private final CombatSettings settings;
    private Consumer<War> warEndHandler;
    private final FactionEventBus eventBus;

    public CombatService(CombatDataStore store, FactionService factionService,
                         TimeProvider timeProvider, ActionLogger actionLogger,
                         CombatSettings settings, FactionEventBus eventBus) {
        this.store = store;
        this.factionService = factionService;
        this.timeProvider = timeProvider;
        this.actionLogger = actionLogger;
        this.settings = settings;
        this.eventBus = eventBus;
    }

    public void setWarEndHandler(Consumer<War> handler) {
        this.warEndHandler = handler;
    }

    /**
     * Enregistre un kill entre deux joueurs.
     * @return Le résultat du kill avec les informations de contexte
     */
    public KillResult recordKill(UUID killerId, UUID victimId) {
        if (killerId == null || victimId == null || killerId.equals(victimId)) {
            return new KillResult(false, null, null, MemberCombatStats.KillType.NEUTRAL, 0);
        }

        long now = timeProvider.nowEpochMs();

        // Get factions
        Optional<Faction> killerFaction = factionService.findFactionByMember(killerId);
        Optional<Faction> victimFaction = factionService.findFactionByMember(victimId);

        // Determine kill type
        MemberCombatStats.KillType killType = determineKillType(killerFaction, victimFaction);

        // Update member stats
        MemberCombatStats killerStats = store.getOrCreateMemberStats(killerId);
        MemberCombatStats victimStats = store.getOrCreateMemberStats(victimId);

        killerStats.recordKill(victimId, killType, now);
        victimStats.recordDeath(killerId, now);

        // Update faction stats if applicable
        if (killerFaction.isPresent()) {
            FactionCombatStats factionStats = store.getOrCreateFactionStats(killerFaction.get().id());
            factionStats.recordKill();
        }
        if (victimFaction.isPresent()) {
            FactionCombatStats factionStats = store.getOrCreateFactionStats(victimFaction.get().id());
            factionStats.recordDeath();
        }

        // Check for active war and award points
        int warPoints = 0;
        if (killerFaction.isPresent() && victimFaction.isPresent()) {
            War war = findActiveWar(killerFaction.get().id(), victimFaction.get().id());
            if (war != null && war.state() == War.WarState.ACTIVE) {
                warPoints = settings.warPointsPerKill;
                war.recordKill(killerFaction.get().id(), warPoints);

                // Check for war end conditions
                checkWarEndConditions(war);
            }
        }

        // Log the kill
        logAction(killerId, "kill victim=" + victimId + " type=" + killType.name());

        return new KillResult(true, killerFaction.orElse(null), victimFaction.orElse(null), killType, warPoints);
    }

    private MemberCombatStats.KillType determineKillType(Optional<Faction> killerFaction, Optional<Faction> victimFaction) {
        if (killerFaction.isEmpty() || victimFaction.isEmpty()) {
            return MemberCombatStats.KillType.NEUTRAL;
        }

        Faction killer = killerFaction.get();
        Faction victim = victimFaction.get();

        if (killer.id().equals(victim.id())) {
            // Same faction - friendly fire
            return MemberCombatStats.KillType.ALLY;
        }

        if (killer.allies().contains(victim.id())) {
            // Allied faction
            return MemberCombatStats.KillType.ALLY;
        }

        if (killer.enemies().contains(victim.id())) {
            // Declared enemy
            return MemberCombatStats.KillType.ENEMY;
        }

        // Different faction, not allied or enemy
        return MemberCombatStats.KillType.FACTION;
    }

    /**
     * Déclare une guerre entre deux factions.
     */
    public Result<War> declareWar(UUID actorId, String targetFactionName) {
        Optional<Faction> attackerOpt = factionService.findFactionByMember(actorId);
        if (attackerOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }

        Faction attacker = attackerOpt.get();
        if (!hasWarPermission(attacker, actorId)) {
            return Result.error("You do not have permission to declare war.");
        }

        Optional<Faction> defenderOpt = factionService.findFactionByName(targetFactionName);
        if (defenderOpt.isEmpty()) {
            return Result.error("Faction not found.");
        }

        Faction defender = defenderOpt.get();
        if (attacker.id().equals(defender.id())) {
            return Result.error("You cannot declare war on yourself.");
        }

        if (attacker.allies().contains(defender.id())) {
            return Result.error("You cannot declare war on an ally. Remove the alliance first.");
        }

        // Check for existing war
        War existingWar = findActiveWar(attacker.id(), defender.id());
        if (existingWar != null) {
            return Result.error("A war is already active between these factions.");
        }

        // Check cooldown
        if (!canDeclareWar(attacker.id())) {
            return Result.error("Your faction must wait before declaring another war.");
        }

        // Create the war
        long now = timeProvider.nowEpochMs();
        long gracePeriodEnd = now + (settings.warGracePeriodMinutes * 60_000L);

        War war = new War(UUID.randomUUID(), attacker.id(), defender.id(), now, gracePeriodEnd);
        store.activeWars().put(war.id(), war);

        // Add as enemies if not already
        if (!attacker.enemies().contains(defender.id())) {
            attacker.addEnemy(defender.id());
            defender.addEnemy(attacker.id());
        }

        logAction(actorId, "war declare attacker=" + attacker.name() + " defender=" + defender.name());
        postEvent(new WarDeclaredEvent(war, attacker, defender, actorId));

        return Result.ok("War declared against " + defender.name() + "! Grace period: " + settings.warGracePeriodMinutes + " minutes.", war);
    }

    /**
     * Accepte une reddition (surrender).
     */
    public Result<Void> surrender(UUID actorId) {
        Optional<Faction> factionOpt = factionService.findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }

        Faction faction = factionOpt.get();
        if (!hasWarPermission(faction, actorId)) {
            return Result.error("You do not have permission to surrender.");
        }

        // Find active war
        War war = findActiveWarForFaction(faction.id());
        if (war == null) {
            return Result.error("Your faction is not at war.");
        }

        // Determine winner
        UUID winnerId = war.attackerFactionId().equals(faction.id()) ? war.defenderFactionId() : war.attackerFactionId();
        UUID loserId = faction.id();

        endWar(war, winnerId.equals(war.attackerFactionId()) ? War.WarResult.ATTACKER_WIN : War.WarResult.DEFENDER_WIN, "Surrender by " + faction.name());

        logAction(actorId, "war surrender faction=" + faction.name());

        return Result.ok("Your faction has surrendered.", null);
    }

    /**
     * Affiche le statut d'une guerre.
     */
    public Optional<War> getActiveWar(UUID factionId) {
        return Optional.ofNullable(findActiveWarForFaction(factionId));
    }

    public List<War> getWarHistory() {
        return List.copyOf(store.warHistory());
    }

    /**
     * Obtient les statistiques d'un membre.
     */
    public MemberCombatStats getMemberStats(UUID playerId) {
        return store.getOrCreateMemberStats(playerId);
    }

    /**
     * Obtient les statistiques d'une faction.
     */
    public FactionCombatStats getFactionStats(UUID factionId) {
        return store.getOrCreateFactionStats(factionId);
    }

    /**
     * Obtient le top des joueurs par kills.
     */
    public List<MemberCombatStats> getTopKillers(int limit) {
        return store.memberStats().values().stream()
                .sorted(Comparator.comparingInt(MemberCombatStats::kills).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * Obtient le top des joueurs par KDR.
     */
    public List<MemberCombatStats> getTopByKdr(int limit) {
        return store.memberStats().values().stream()
                .filter(stats -> stats.kills() + stats.deaths() >= 10) // Minimum games
                .sorted(Comparator.comparingDouble(MemberCombatStats::kdr).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * Obtient le top des factions par kills.
     */
    public List<FactionCombatStats> getTopFactions(int limit) {
        return store.factionStats().values().stream()
                .sorted(Comparator.comparingInt(FactionCombatStats::totalKills).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * Met à jour les guerres (appelé périodiquement).
     */
    public void tickWars() {
        long now = timeProvider.nowEpochMs();
        List<War> toEnd = new ArrayList<>();

        for (War war : store.activeWars().values()) {
            // Activate wars past grace period
            if (war.state() == War.WarState.PENDING && now >= war.gracePeriodEnd()) {
                war.activate();
                postEvent(new WarStartedEvent(war,
                        factionService.getFactionById(war.attackerFactionId()).orElse(null),
                        factionService.getFactionById(war.defenderFactionId()).orElse(null)));
            }

            // Check for war duration limit
            if (war.state() == War.WarState.ACTIVE && settings.warDurationMinutes > 0) {
                long warDuration = now - war.gracePeriodEnd();
                if (warDuration >= settings.warDurationMinutes * 60_000L) {
                    toEnd.add(war);
                }
            }
        }

        // End wars that exceeded duration
        for (War war : toEnd) {
            checkWarEndConditions(war);
            if (war.state() == War.WarState.ACTIVE) {
                // Force end by time
                War.WarResult result;
                if (war.attackerPoints() > war.defenderPoints()) {
                    result = War.WarResult.ATTACKER_WIN;
                } else if (war.defenderPoints() > war.attackerPoints()) {
                    result = War.WarResult.DEFENDER_WIN;
                } else {
                    result = War.WarResult.DRAW;
                }
                endWar(war, result, "Time limit reached");
            }
        }
    }

    private War findActiveWar(UUID faction1Id, UUID faction2Id) {
        for (War war : store.activeWars().values()) {
            if (war.state() == War.WarState.ENDED) {
                continue;
            }
            if ((war.attackerFactionId().equals(faction1Id) && war.defenderFactionId().equals(faction2Id)) ||
                (war.attackerFactionId().equals(faction2Id) && war.defenderFactionId().equals(faction1Id))) {
                return war;
            }
        }
        return null;
    }

    private War findActiveWarForFaction(UUID factionId) {
        for (War war : store.activeWars().values()) {
            if (war.state() == War.WarState.ENDED) {
                continue;
            }
            if (war.isParticipant(factionId)) {
                return war;
            }
        }
        return null;
    }

    private void checkWarEndConditions(War war) {
        // Check if either faction reached the point goal
        if (settings.warPointsToWin > 0) {
            if (war.attackerPoints() >= settings.warPointsToWin) {
                endWar(war, War.WarResult.ATTACKER_WIN, "Point goal reached");
            } else if (war.defenderPoints() >= settings.warPointsToWin) {
                endWar(war, War.WarResult.DEFENDER_WIN, "Point goal reached");
            }
        }
    }

    private void endWar(War war, War.WarResult result, String reason) {
        long now = timeProvider.nowEpochMs();
        war.end(result, reason, now);

        // Update faction stats
        FactionCombatStats attackerStats = store.getOrCreateFactionStats(war.attackerFactionId());
        FactionCombatStats defenderStats = store.getOrCreateFactionStats(war.defenderFactionId());

        switch (result) {
            case ATTACKER_WIN -> {
                attackerStats.recordWarWin();
                defenderStats.recordWarLoss();
            }
            case DEFENDER_WIN -> {
                attackerStats.recordWarLoss();
                defenderStats.recordWarWin();
            }
            case DRAW -> {
                attackerStats.recordWarDraw();
                defenderStats.recordWarDraw();
            }
            default -> {} // SURRENDER, CANCELLED - handled separately
        }

        postEvent(new WarEndedEvent(war,
                factionService.getFactionById(war.attackerFactionId()).orElse(null),
                factionService.getFactionById(war.defenderFactionId()).orElse(null),
                result, reason));

        // Move to history
        store.activeWars().remove(war.id());
        store.warHistory().add(war);

        // Keep only recent history
        while (store.warHistory().size() > settings.warHistoryLimit) {
            store.warHistory().remove(0);
        }

        if (warEndHandler != null) {
            warEndHandler.accept(war);
        }
    }

    private void postEvent(com.kingc.hytale.factions.api.event.FactionEvent event) {
        if (eventBus == null || event == null) {
            return;
        }
        eventBus.post(event);
    }

    private boolean hasWarPermission(Faction faction, UUID playerId) {
        var role = faction.roleOf(playerId);
        return role != null && role.atLeast(settings.roleForWar);
    }

    private boolean canDeclareWar(UUID factionId) {
        // Check if faction recently ended a war
        long now = timeProvider.nowEpochMs();
        for (War war : store.warHistory()) {
            if (war.attackerFactionId().equals(factionId) || war.defenderFactionId().equals(factionId)) {
                long cooldownEnd = war.endTime() + (settings.warCooldownMinutes * 60_000L);
                if (now < cooldownEnd) {
                    return false;
                }
            }
        }
        return true;
    }

    private void logAction(UUID actorId, String action) {
        if (actionLogger == null || action == null || action.isBlank()) {
            return;
        }
        String actor = actorId == null ? "system" : actorId.toString();
        actionLogger.log("actor=" + actor + " " + action);
    }

    /**
     * Résultat d'un enregistrement de kill.
     */
    public record KillResult(
            boolean success,
            Faction killerFaction,
            Faction victimFaction,
            MemberCombatStats.KillType killType,
            int warPoints
    ) {}

    /**
     * Configuration du système de combat.
     */
    public static final class CombatSettings {
        public int warPointsPerKill = 10;
        public int warPointsToWin = 100;
        public int warGracePeriodMinutes = 5;
        public int warDurationMinutes = 60;
        public int warCooldownMinutes = 30;
        public int warHistoryLimit = 50;
        public com.kingc.hytale.factions.model.MemberRole roleForWar = com.kingc.hytale.factions.model.MemberRole.LEADER;
    }
}
