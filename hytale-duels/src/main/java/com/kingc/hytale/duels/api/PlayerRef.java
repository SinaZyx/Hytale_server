package com.kingc.hytale.duels.api;

import java.util.Optional;
import java.util.UUID;

public interface PlayerRef extends CommandSource {
    UUID id();
    String name();
    void sendMessage(String message);
    void teleport(Location location);
    boolean isOnline();

    // Default implementations for CommandSource compliance
    @Override
    default Optional<UUID> playerId() {
        return Optional.of(id());
    }

    @Override
    default Optional<PlayerRef> player() {
        return Optional.of(this);
    }

    @Override
    default boolean hasPermission(String permission) {
        return true; // Default, can be overridden by implementations checking permissions
    }
}
