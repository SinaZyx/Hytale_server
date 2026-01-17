package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;

public final class RankingCommand extends CommandBase {
    private final HytaleDuelsPlugin plugin;

    public RankingCommand(HytaleDuelsPlugin plugin) {
        super("ranking", "Open ranking menu");
        this.plugin = plugin;
        this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            return;
        }
        
        PlayerRef playerRef = Universe.get().getPlayer(ctx.sender().getUuid());
        if (playerRef != null) {
            plugin.openRankingMenu(playerRef);
        }
    }
}
