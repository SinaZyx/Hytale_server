package com.kingc.hytale.factions.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kingc.hytale.factions.model.FactionCombatStats;
import com.kingc.hytale.factions.model.MemberCombatStats;
import com.kingc.hytale.factions.model.War;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stockage des données de combat (stats joueurs, stats factions, guerres).
 */
public final class CombatDataStore {
    private final Path path;
    private final Gson gson;

    private final Map<UUID, MemberCombatStats> memberStats = new HashMap<>();
    private final Map<UUID, FactionCombatStats> factionStats = new HashMap<>();
    private final Map<UUID, War> activeWars = new HashMap<>();
    private final List<War> warHistory = new ArrayList<>();

    public CombatDataStore(Path path) {
        this.path = path;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public Map<UUID, MemberCombatStats> memberStats() {
        return memberStats;
    }

    public Map<UUID, FactionCombatStats> factionStats() {
        return factionStats;
    }

    public Map<UUID, War> activeWars() {
        return activeWars;
    }

    public List<War> warHistory() {
        return warHistory;
    }

    public MemberCombatStats getOrCreateMemberStats(UUID playerId) {
        return memberStats.computeIfAbsent(playerId, MemberCombatStats::new);
    }

    public FactionCombatStats getOrCreateFactionStats(UUID factionId) {
        return factionStats.computeIfAbsent(factionId, FactionCombatStats::new);
    }

    public void save() throws IOException {
        StoredCombatData stored = new StoredCombatData();

        // Save member stats
        for (MemberCombatStats stats : memberStats.values()) {
            MemberStatsRecord record = new MemberStatsRecord();
            record.playerId = stats.playerId().toString();
            record.kills = stats.kills();
            record.deaths = stats.deaths();
            record.factionKills = stats.factionKills();
            record.allyKills = stats.allyKills();
            record.enemyKills = stats.enemyKills();
            record.currentStreak = stats.currentStreak();
            record.bestStreak = stats.bestStreak();
            record.lastKilledBy = stats.lastKilledBy() == null ? null : stats.lastKilledBy().toString();
            record.lastKilled = stats.lastKilled() == null ? null : stats.lastKilled().toString();
            record.lastDeathTime = stats.lastDeathTime();
            record.lastKillTime = stats.lastKillTime();
            stored.memberStats.put(record.playerId, record);
        }

        // Save faction stats
        for (FactionCombatStats stats : factionStats.values()) {
            FactionStatsRecord record = new FactionStatsRecord();
            record.factionId = stats.factionId().toString();
            record.totalKills = stats.totalKills();
            record.totalDeaths = stats.totalDeaths();
            record.warsWon = stats.warsWon();
            record.warsLost = stats.warsLost();
            record.warsDraw = stats.warsDraw();
            record.territoriesCaptured = stats.territoriesCaptured();
            record.territoriesLost = stats.territoriesLost();
            stored.factionStats.put(record.factionId, record);
        }

        // Save active wars
        for (War war : activeWars.values()) {
            stored.activeWars.put(war.id().toString(), toWarRecord(war));
        }

        // Save war history
        for (War war : warHistory) {
            stored.warHistory.add(toWarRecord(war));
        }

        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(stored, writer);
        }
    }

    private WarRecord toWarRecord(War war) {
        WarRecord record = new WarRecord();
        record.id = war.id().toString();
        record.attackerFactionId = war.attackerFactionId().toString();
        record.defenderFactionId = war.defenderFactionId().toString();
        record.startTime = war.startTime();
        record.gracePeriodEnd = war.gracePeriodEnd();
        record.attackerPoints = war.attackerPoints();
        record.defenderPoints = war.defenderPoints();
        record.attackerKills = war.attackerKills();
        record.defenderKills = war.defenderKills();
        record.state = war.state().name();
        record.result = war.result() == null ? null : war.result().name();
        record.endReason = war.endReason();
        record.endTime = war.endTime();
        return record;
    }

    private void load() {
        if (!Files.exists(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            StoredCombatData stored = gson.fromJson(reader, StoredCombatData.class);
            if (stored == null) {
                return;
            }

            // Load member stats
            if (stored.memberStats != null) {
                for (MemberStatsRecord record : stored.memberStats.values()) {
                    UUID playerId = UUID.fromString(record.playerId);
                    MemberCombatStats stats = new MemberCombatStats(playerId);
                    stats.setStats(
                            record.kills,
                            record.deaths,
                            record.factionKills,
                            record.allyKills,
                            record.enemyKills,
                            record.currentStreak,
                            record.bestStreak,
                            record.lastKilledBy == null ? null : UUID.fromString(record.lastKilledBy),
                            record.lastKilled == null ? null : UUID.fromString(record.lastKilled),
                            record.lastDeathTime,
                            record.lastKillTime
                    );
                    memberStats.put(playerId, stats);
                }
            }

            // Load faction stats
            if (stored.factionStats != null) {
                for (FactionStatsRecord record : stored.factionStats.values()) {
                    UUID factionId = UUID.fromString(record.factionId);
                    FactionCombatStats stats = new FactionCombatStats(factionId);
                    stats.setStats(
                            record.totalKills,
                            record.totalDeaths,
                            record.warsWon,
                            record.warsLost,
                            record.warsDraw,
                            record.territoriesCaptured,
                            record.territoriesLost
                    );
                    factionStats.put(factionId, stats);
                }
            }

            // Load active wars
            if (stored.activeWars != null) {
                for (WarRecord record : stored.activeWars.values()) {
                    War war = fromWarRecord(record);
                    activeWars.put(war.id(), war);
                }
            }

            // Load war history
            if (stored.warHistory != null) {
                for (WarRecord record : stored.warHistory) {
                    warHistory.add(fromWarRecord(record));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load combat data", e);
        }
    }

    private War fromWarRecord(WarRecord record) {
        War war = new War(
                UUID.fromString(record.id),
                UUID.fromString(record.attackerFactionId),
                UUID.fromString(record.defenderFactionId),
                record.startTime,
                record.gracePeriodEnd
        );
        war.setStats(
                record.attackerPoints,
                record.defenderPoints,
                record.attackerKills,
                record.defenderKills,
                War.WarState.valueOf(record.state),
                record.result == null ? null : War.WarResult.valueOf(record.result),
                record.endReason,
                record.endTime
        );
        return war;
    }

    // Storage records
    public static final class StoredCombatData {
        public Map<String, MemberStatsRecord> memberStats = new HashMap<>();
        public Map<String, FactionStatsRecord> factionStats = new HashMap<>();
        public Map<String, WarRecord> activeWars = new HashMap<>();
        public List<WarRecord> warHistory = new ArrayList<>();
    }

    public static final class MemberStatsRecord {
        public String playerId;
        public int kills;
        public int deaths;
        public int factionKills;
        public int allyKills;
        public int enemyKills;
        public int currentStreak;
        public int bestStreak;
        public String lastKilledBy;
        public String lastKilled;
        public long lastDeathTime;
        public long lastKillTime;
    }

    public static final class FactionStatsRecord {
        public String factionId;
        public int totalKills;
        public int totalDeaths;
        public int warsWon;
        public int warsLost;
        public int warsDraw;
        public int territoriesCaptured;
        public int territoriesLost;
    }

    public static final class WarRecord {
        public String id;
        public String attackerFactionId;
        public String defenderFactionId;
        public long startTime;
        public long gracePeriodEnd;
        public int attackerPoints;
        public int defenderPoints;
        public int attackerKills;
        public int defenderKills;
        public String state;
        public String result;
        public String endReason;
        public long endTime;
    }
}
