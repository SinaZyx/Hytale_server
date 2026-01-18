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
        
        // Intercept help command to show formatted help in chat
        String[] parts = commandLine.split("\\s+", 2);
        if (parts.length >= 2 && "help".equalsIgnoreCase(parts[1])) {
            if (ctx.isPlayer() && ctx.sender() != null) {
                sendHelpMessage(ctx);
                return;
            }
        }
        
        plugin.core().onCommand(new HytaleCommandSource(ctx), commandLine);
        plugin.flush();
    }
    
    private void sendHelpMessage(CommandContext ctx) {
        ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(""));
        ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw("&6&l═══ ⚔ DUELS - AIDE ═══"));
        ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(""));
        ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw("&e/duel <joueur> [kit] &7- Défier un joueur"));
        ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw("&e/duel accept &7- Accepter un défi"));
        ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw("&e/duel decline &7- Refuser un défi"));
        ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(""));
        ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw("&e/duel top &7- Voir le classement"));
        ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw("&e/duel stats [joueur] &7- Voir les statistiques"));
        ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(""));
        ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw("&e/kit list &7- Liste des kits disponibles"));
        ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw("&e/kit save <nom> &7- Créer un kit (admin)"));
        ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(""));
        ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw("&6&l══════════════════════"));
    }
}
