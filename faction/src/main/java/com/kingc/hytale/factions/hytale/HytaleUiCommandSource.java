package com.kingc.hytale.factions.hytale;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.kingc.hytale.factions.api.CommandSource;
import com.kingc.hytale.factions.integration.FancyCoreBridge;

import java.util.Optional;
import java.util.UUID;

public final class HytaleUiCommandSource implements CommandSource {
    private final PlayerRef playerRef;

    public HytaleUiCommandSource(PlayerRef playerRef) {
        this.playerRef = playerRef;
    }

    @Override
    public void sendMessage(String message) {
        playerRef.sendMessage(Message.raw(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        var fancyCheck = FancyCoreBridge.checkPermission(playerRef.getUuid(), permission);
        if (fancyCheck.isPresent()) {
            return fancyCheck.get();
        }
        return true;
    }

    @Override
    public Optional<UUID> playerId() {
        return Optional.ofNullable(playerRef.getUuid());
    }
}
