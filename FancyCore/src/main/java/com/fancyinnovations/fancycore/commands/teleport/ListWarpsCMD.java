package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.api.teleport.Warp;
import com.fancyinnovations.fancycore.api.teleport.WarpService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ListWarpsCMD extends CommandBase {

    public ListWarpsCMD() {
        super("listwarps", "Lists all available warp points on the server");
        addAliases("warps");
        requirePermission("fancycore.commands.listwarps");
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

        List<Warp> warps = WarpService.get().getAllWarps();
        if (warps == null || warps.isEmpty()) {
            ctx.sendMessage(Message.raw("No warps have been created yet."));
            return;
        }

        ctx.sendMessage(Message.raw("Available Warps:"));
        for (Warp warp : warps) {
            if (!PermissionsModule.get().hasPermission(fp.getData().getUUID(), "fancycore.warps." + warp.name())) {
                continue;
            }
            ctx.sendMessage(Message.raw("- " + warp.name()));
        }
    }
}
