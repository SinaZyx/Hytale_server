package com.kingc.hytale.factions.api;

import java.util.Optional;
import java.util.UUID;

public interface PlayerRef extends CommandSource {
    UUID id();

    String name();

    @Override
    default Optional<UUID> playerId() {
        return Optional.of(id());
    }
}
