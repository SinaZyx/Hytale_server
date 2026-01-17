package com.kingc.hytale.duels.arena;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class ArenaService {
    private final ArenaRepository repository;
    private final Set<String> occupiedArenas = new HashSet<>();

    public ArenaService(ArenaRepository repository) {
        this.repository = repository;
    }

    public Optional<Arena> getArena(String id) {
        return repository.get(id);
    }

    public Collection<Arena> getAllArenas() {
        return repository.getAll();
    }

    public Optional<Arena> findAvailableArena(int requiredPlayers) {
        return repository.getAll().stream()
            .filter(arena -> !occupiedArenas.contains(arena.id()))
            .filter(arena -> arena.maxPlayers() >= requiredPlayers)
            .findFirst();
    }

    public boolean reserveArena(String arenaId) {
        if (occupiedArenas.contains(arenaId)) {
            return false;
        }
        occupiedArenas.add(arenaId);
        return true;
    }

    public void releaseArena(String arenaId) {
        occupiedArenas.remove(arenaId);
    }

    public boolean isArenaAvailable(String arenaId) {
        return !occupiedArenas.contains(arenaId);
    }

    public void addArena(Arena arena) {
        repository.add(arena);
    }

    public boolean removeArena(String id) {
        if (occupiedArenas.contains(id)) {
            return false;
        }
        return repository.remove(id);
    }

    public void save() throws IOException {
        repository.save();
    }
}
