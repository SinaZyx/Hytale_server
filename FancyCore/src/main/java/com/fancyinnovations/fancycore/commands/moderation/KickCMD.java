package com.fancyinnovations.fancycore.commands.moderation;

import com.fancyinnovations.fancycore.api.moderation.PunishmentService;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.commands.arguments.FancyCoreArgs;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class KickCMD extends CommandBase {

    protected final RequiredArg<FancyPlayer> targetArg = this.withRequiredArg("target", "The player to kick", FancyCoreArgs.PLAYER);
    protected final RequiredArg<List<String>> reasonArg = this.withListRequiredArg("reason", "The reason for the kick", ArgTypes.STRING);

    public KickCMD() {
        super("kick", "Kick a player from the server");
        requirePermission("fancycore.commands.kick");
        setAllowsExtraArguments(true);
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be executed by a player."));
            return;
        }

        FancyPlayer fp = FancyPlayerService.get().getByUUID(ctx.sender().getUuid());
        if (fp == null) {
            ctx.sendMessage(Message.raw("FancyPlayer not found."));
            return;
        }

        FancyPlayer target = targetArg.get(ctx);
        if (!target.isOnline()) {
            fp.sendMessage("The player " + target.getData().getUsername() + " is not online.");
            return;
        }

        String[] parts = ctx.getInputString().split(" ");
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            reasonBuilder.append(parts[i]);
            if (i < parts.length - 1) {
                reasonBuilder.append(" ");
            }
        }
        String reason = reasonBuilder.toString();

        PunishmentService.get().kickPlayer(target, fp, reason);

        fp.sendMessage("Successfully kicked " + target.getData().getUsername() + " for: " + reason);
    }
}
