package com.kingc.hytale.factions.api;

import java.util.Optional;
import java.util.UUID;

public interface ServerAdapter {
    Optional<PlayerRef> findOnlinePlayerByName(String name);

    Optional<PlayerRef> findOnlinePlayer(UUID id);

    Optional<UUID> resolvePlayerId(String name);

    Optional<String> resolvePlayerName(UUID id);

    Optional<Location> getPlayerLocation(UUID id);

    void sendMessage(UUID id, String message);

    long nowEpochMs();
}
