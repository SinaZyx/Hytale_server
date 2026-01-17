package com.kingc.hytale.factions.hytale;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

public final class FactionsCommand extends CommandBase {
    private final HytaleFactionsPlugin plugin;

    public FactionsCommand(HytaleFactionsPlugin plugin) {
        super("f", "Faction commands");
        this.plugin = plugin;
        this.addAliases("faction", "factions");
        this.setPermissionGroup(GameMode.Adventure);
        this.setAllowsExtraArguments(true);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        plugin.handleCommand(ctx);
        plugin.flush();
    }
}
