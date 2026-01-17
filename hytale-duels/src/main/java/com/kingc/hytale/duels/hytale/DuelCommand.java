package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

public final class DuelCommand extends CommandBase {
    private final HytaleDuelsPlugin plugin;

    public DuelCommand(HytaleDuelsPlugin plugin) {
        super("duel", "Duel another player");
        this.plugin = plugin;
        this.setPermissionGroup(GameMode.Adventure);
        this.setAllowsExtraArguments(true);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        String input = ctx.getInputString();
        String commandLine = "duel";
        if (input != null && input.length() > 5) { // "duel " is 5 chars
            commandLine = input.trim();
        }
        plugin.core().onCommand(new HytaleCommandSource(ctx), commandLine);
        plugin.flush();
    }
}
