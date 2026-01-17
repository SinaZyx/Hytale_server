package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

public final class TopCommand extends CommandBase {
    private final HytaleDuelsPlugin plugin;

    public TopCommand(HytaleDuelsPlugin plugin) {
        super("top", "View leaderboard");
        this.plugin = plugin;
        this.addAliases("leaderboard");
        this.setPermissionGroup(GameMode.Adventure);
        this.setAllowsExtraArguments(true);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
         String input = ctx.getInputString();
        // Handle alias replacement if necessary, but simpler to just rebuild command
        String[] parts = input.trim().split(" ");
        String commandLine = "top";
        if (parts.length > 1) {
             commandLine += " " + input.substring(parts[0].length()).trim();
        }
        
        plugin.core().onCommand(new HytaleCommandSource(ctx), commandLine);
        plugin.flush();
    }
}
