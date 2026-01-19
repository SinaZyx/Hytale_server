package com.kingc.hytale.factions.hytale;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.kingc.hytale.factions.api.CommandSource;
import com.kingc.hytale.factions.integration.FancyCoreBridge;

import java.util.Optional;
import java.util.UUID;

public final class HytaleCommandSource implements CommandSource {
    private final CommandContext ctx;

    public HytaleCommandSource(CommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void sendMessage(String message) {
        ctx.sendMessage(Message.raw(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        CommandSender sender = ctx.sender();
        if (sender == null) {
            return false;
        }
        var fancyCheck = FancyCoreBridge.checkPermission(sender.getUuid(), permission);
        if (fancyCheck.isPresent()) {
            return fancyCheck.get();
        }
        return sender.hasPermission(permission);
    }

    @Override
    public Optional<UUID> playerId() {
        if (!ctx.isPlayer()) {
            return Optional.empty();
        }
        CommandSender sender = ctx.sender();
        if (sender == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sender.getUuid());
    }
}
