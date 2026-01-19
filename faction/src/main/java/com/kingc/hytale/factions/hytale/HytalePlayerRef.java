package com.kingc.hytale.factions.hytale;

import com.hypixel.hytale.server.core.Message;
import com.kingc.hytale.factions.api.PlayerRef;
import com.kingc.hytale.factions.integration.FancyCoreBridge;

import java.util.UUID;

public final class HytalePlayerRef implements PlayerRef {
    private final com.hypixel.hytale.server.core.universe.PlayerRef ref;

    public HytalePlayerRef(com.hypixel.hytale.server.core.universe.PlayerRef ref) {
        this.ref = ref;
    }

    public com.hypixel.hytale.server.core.universe.PlayerRef ref() {
        return ref;
    }

    @Override
    public void sendMessage(String message) {
        ref.sendMessage(Message.raw(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        var fancyCheck = FancyCoreBridge.checkPermission(ref.getUuid(), permission);
        if (fancyCheck.isPresent()) {
            return fancyCheck.get();
        }
        return true;
    }

    @Override
    public UUID id() {
        return ref.getUuid();
    }

    @Override
    public String name() {
        return ref.getUsername();
    }
}
