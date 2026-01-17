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
        
        // Intercept help command to open UI
        String[] parts = commandLine.split("\\s+", 2);
        if (parts.length >= 2 && "help".equalsIgnoreCase(parts[1])) {
            if (ctx.isPlayer() && ctx.sender() != null) {
                var playerRef = com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(ctx.sender().getUuid());
                if (playerRef != null) {
                    plugin.openHelpMenu(playerRef);
                    return;
                }
            }
        }
        
        plugin.core().onCommand(new HytaleCommandSource(ctx), commandLine);
        plugin.flush();
    }
}
