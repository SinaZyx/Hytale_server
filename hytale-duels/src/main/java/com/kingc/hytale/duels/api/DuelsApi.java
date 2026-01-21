package com.kingc.hytale.duels.api;

import com.kingc.hytale.duels.api.event.DuelEvent;
import com.kingc.hytale.duels.kit.KitDefinition;
import com.kingc.hytale.duels.match.Match;
import com.kingc.hytale.duels.ranking.PlayerStats;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public interface DuelsApi {
    // Stats
    Optional<PlayerStats> getPlayerStats(UUID playerId);
    List<PlayerStats> getTopPlayers(int limit);

    // Matches
    Optional<Match> getActiveMatch(UUID playerId);
    java.util.Collection<Match> getActiveMatches();

    // Kits
    List<KitDefinition> getKits();
    Optional<KitDefinition> getKit(String kitId);

    // Events
    <T extends DuelEvent> void registerEventHandler(Class<T> eventClass, Consumer<T> handler);
}
