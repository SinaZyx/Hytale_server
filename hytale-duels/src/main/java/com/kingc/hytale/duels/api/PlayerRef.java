package com.kingc.hytale.duels.api;

import java.util.UUID;

public interface PlayerRef {
    UUID id();
    String name();
    void sendMessage(String message);
    void teleport(Location location);
    boolean isOnline();
}
