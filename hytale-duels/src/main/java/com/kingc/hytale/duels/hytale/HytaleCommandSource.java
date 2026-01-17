package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.kingc.hytale.duels.api.CommandSource;

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

    @Override
    public Optional<com.kingc.hytale.duels.api.PlayerRef> player() {
        if (!ctx.isPlayer()) {
            return Optional.empty();
        }
        CommandSender sender = ctx.sender();
        if (sender == null) {
            return Optional.empty();
        }
        PlayerRef ref = Universe.get().getPlayer(sender.getUuid());
        if (ref == null) {
            return Optional.empty();
        }
        return Optional.of(new HytalePlayerRef(ref));
    }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }
}
