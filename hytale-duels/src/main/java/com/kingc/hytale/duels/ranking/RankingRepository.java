package com.kingc.hytale.duels.ranking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RankingRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path filePath;
    private final Map<UUID, PlayerStats> stats = new ConcurrentHashMap<>();
    private boolean dirty;

    public RankingRepository(Path filePath) throws IOException {
        this.filePath = filePath;
        load();
    }

    private void load() throws IOException {
        if (!Files.exists(filePath)) {
            save();
            return;
        }
        String json = Files.readString(filePath);
        Map<String, PlayerStats> loaded = GSON.fromJson(json, new TypeToken<Map<String, PlayerStats>>() {}.getType());
        if (loaded != null) {
            for (Map.Entry<String, PlayerStats> entry : loaded.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    stats.put(uuid, entry.getValue());
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void save() throws IOException {
        Files.createDirectories(filePath.getParent());
        Map<String, PlayerStats> toSave = new ConcurrentHashMap<>();
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            toSave.put(entry.getKey().toString(), entry.getValue());
        }
        Files.writeString(filePath, GSON.toJson(toSave));
        dirty = false;
    }

    public void saveIfDirty() throws IOException {
        if (dirty) {
            save();
        }
    }

    public Optional<PlayerStats> get(UUID playerId) {
        return Optional.ofNullable(stats.get(playerId));
    }

    public PlayerStats getOrCreate(UUID playerId, String playerName) {
        return stats.computeIfAbsent(playerId, id -> {
            dirty = true;
            return new PlayerStats(id, playerName);
        });
    }

    public void update(PlayerStats playerStats) {
        stats.put(playerStats.playerId(), playerStats);
        dirty = true;
    }

    public List<PlayerStats> getAll() {
        return new ArrayList<>(stats.values());
    }

    public List<PlayerStats> getTopPlayers(int limit) {
        return stats.values().stream()
            .sorted(Comparator.comparingInt(PlayerStats::elo).reversed())
            .limit(limit)
            .toList();
    }

    public List<PlayerStats> getTopPlayersByWins(int limit) {
        return stats.values().stream()
            .sorted(Comparator.comparingInt(PlayerStats::wins).reversed())
            .limit(limit)
            .toList();
    }

    public List<PlayerStats> getTopPlayersByWinRate(int limit, int minMatches) {
        return stats.values().stream()
            .filter(s -> s.totalMatches() >= minMatches)
            .sorted(Comparator.comparingDouble(PlayerStats::winRate).reversed())
            .limit(limit)
            .toList();
    }

    public int getRank(UUID playerId) {
        List<PlayerStats> sorted = stats.values().stream()
            .sorted(Comparator.comparingInt(PlayerStats::elo).reversed())
            .toList();

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).playerId().equals(playerId)) {
                return i + 1;
            }
        }
        return -1;
    }

    public int getTotalPlayers() {
        return stats.size();
    }

    public int getPlayersInRank(Rank rank) {
        return (int) stats.values().stream()
            .filter(s -> s.getRank() == rank)
            .count();
    }
}
