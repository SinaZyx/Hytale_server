package com.kingc.hytale.duels.api;

import java.util.Optional;
import java.util.UUID;

public interface CommandSource {
    void sendMessage(String message);
    Optional<UUID> playerId();
    Optional<PlayerRef> player();
    boolean hasPermission(String permission);
}
