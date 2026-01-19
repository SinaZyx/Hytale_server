package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;

public final class DuelsAdminCommand extends CommandBase {
    private final HytaleDuelsPlugin plugin;

    public DuelsAdminCommand(HytaleDuelsPlugin plugin) {
        super("duelsadmin", "Admin menu");
        this.plugin = plugin;
        this.setPermissionGroup(GameMode.Creative); // Admin only assumption
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            return;
        }

        PlayerRef playerRef = Universe.get().getPlayer(ctx.sender().getUuid());
         if (playerRef != null) {
             // TODO: Check extra admin permission if needed
            plugin.openAdminMenu(playerRef);
        }
    }
}
