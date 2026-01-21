package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.teleport.Warp;
import com.fancyinnovations.fancycore.api.teleport.WarpService;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class DeleteWarpCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();
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
            translator.getMessage("teleport.warp.name_empty").sendTo(ctx.sender());
            return;
        }

        Warp warp = WarpService.get().getWarp(warpName);
        if (warp == null) {
            translator.getMessage("teleport.warp.not_found")
                .replace("name", warpName)
                .sendTo(ctx.sender());
            return;
        }

        WarpService.get().deleteWarp(warp.name());

        translator.getMessage("teleport.warp.deleted")
            .replace("name", warpName)
            .sendTo(ctx.sender());
    }
}
