package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

public final class KitCommand extends CommandBase {
    private final HytaleDuelsPlugin plugin;

    public KitCommand(HytaleDuelsPlugin plugin) {
        super("kit", "Manage kits");
        this.plugin = plugin;
        this.setPermissionGroup(GameMode.Adventure);
        this.setAllowsExtraArguments(true);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
         String input = ctx.getInputString();
        String commandLine = "kit";
        if (input != null && input.length() > 4) { 
            commandLine = input.trim();
        }
        plugin.core().onCommand(new HytaleCommandSource(ctx), commandLine);
        plugin.flush();
    }
}
