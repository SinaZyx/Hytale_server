package com.fancyinnovations.fancycore.commands;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

public abstract class FancyLeafCommandBase extends CommandBase {

    protected FancyLeafCommandBase(String name, String description) {
        super(name, description);
    }

    protected boolean checkPermission(CommandContext ctx, String permission) {
        if (ctx.sender().hasPermission(permission)) {
            return true;
        }

        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("You do not have permission to use this command."));
            return false;
        }

        FancyPlayer player = FancyCorePlugin.get().getPlayerService().getByUUID(ctx.sender().getUuid());
        if (player == null) {
            ctx.sendMessage(Message.raw("You do not have permission to use this command."));
            return false;
        }

        String language = player.getLanguage();
        FancyCorePlugin.get().getTranslationService()
                .getMessage("error.permission", language)
                .sendTo(player);

        return false;
    }
}
