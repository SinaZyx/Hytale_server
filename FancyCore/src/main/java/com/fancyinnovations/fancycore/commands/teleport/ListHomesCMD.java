package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.api.player.Home;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class ListHomesCMD extends CommandBase {

    public ListHomesCMD() {
        super("listhomes", "Lists all your home points");
        addAliases("homes");
        requirePermission("fancycore.commands.listhomes");
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

        fp.sendMessage("Your Homes:");
        for (Home home : fp.getData().getHomes()) {
            fp.sendMessage("- " + home.name() + " (World: " + home.location().worldName() + ", X: " + home.location().x() + ", Y: " + home.location().y() + ", Z: " + home.location().z() + ")");
        }
    }
}
