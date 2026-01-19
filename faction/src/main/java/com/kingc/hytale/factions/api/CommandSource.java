package com.kingc.hytale.factions.api;

import java.util.Optional;
import java.util.UUID;

public interface CommandSource {
    void sendMessage(String message);

    boolean hasPermission(String permission);

    Optional<UUID> playerId();
}
