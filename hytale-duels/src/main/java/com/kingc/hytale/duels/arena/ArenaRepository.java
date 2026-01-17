package com.kingc.hytale.duels.arena;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kingc.hytale.duels.api.Location;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ArenaRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path filePath;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private boolean dirty;

    public ArenaRepository(Path filePath) throws IOException {
        this.filePath = filePath;
        load();
    }

    private void load() throws IOException {
        if (!Files.exists(filePath)) {
            createDefaults();
            save();
            return;
        }
        String json = Files.readString(filePath);
        Map<String, Arena> loaded = GSON.fromJson(json, new TypeToken<Map<String, Arena>>() {}.getType());
        if (loaded != null) {
            arenas.putAll(loaded);
        }
    }

    private void createDefaults() {
        add(Arena.builder("arena1")
            .displayName("Arena 1")
            .team1Spawns(List.of(new Location("world", 0, 64, 10)))
            .team2Spawns(List.of(new Location("world", 0, 64, -10)))
            .spectatorSpawn(new Location("world", 20, 70, 0))
            .maxPlayers(2)
            .build());

        add(Arena.builder("arena2")
            .displayName("Arena 2")
            .team1Spawns(List.of(
                new Location("world", 50, 64, 10),
                new Location("world", 55, 64, 10)
            ))
            .team2Spawns(List.of(
                new Location("world", 50, 64, -10),
                new Location("world", 55, 64, -10)
            ))
            .spectatorSpawn(new Location("world", 70, 70, 0))
            .maxPlayers(4)
            .build());
    }

    public void save() throws IOException {
        if (!dirty && Files.exists(filePath)) {
            return;
        }
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, GSON.toJson(arenas));
        dirty = false;
    }

    public Optional<Arena> get(String id) {
        return Optional.ofNullable(arenas.get(id.toLowerCase()));
    }

    public Collection<Arena> getAll() {
        return arenas.values();
    }

    public void add(Arena arena) {
        arenas.put(arena.id().toLowerCase(), arena);
        dirty = true;
    }

    public boolean remove(String id) {
        boolean removed = arenas.remove(id.toLowerCase()) != null;
        if (removed) {
            dirty = true;
        }
        return removed;
    }
}
