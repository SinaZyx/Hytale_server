package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.teleport.Warp;
import com.fancyinnovations.fancycore.api.teleport.WarpService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class DeleteWarpCMD extends CommandBase {

    protected final RequiredArg<String> nameArg = this.withRequiredArg("warp", "name of the warp", ArgTypes.STRING);

    public DeleteWarpCMD() {
        super("deletewarp", "Deletes the warp point with the specified name");
        addAliases("delwarp");
        requirePermission("fancycore.commands.deletewarp");
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        String warpName = nameArg.get(ctx);
        if (warpName == null || warpName.trim().isEmpty()) {
            ctx.sendMessage(Message.raw("Warp name cannot be empty."));
            return;
        }

        Warp warp = WarpService.get().getWarp(warpName);
        if (warp == null) {
            ctx.sendMessage(Message.raw("Warp \"" + warpName + "\" does not exist."));
            return;
        }

        // Delete warp
        WarpService.get().deleteWarp(warp.name());

        // Send success message
        ctx.sendMessage(Message.raw("Warp \"" + warpName + "\" deleted."));
    }
}
