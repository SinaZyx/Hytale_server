package com.fancyinnovations.fancycore.commands.inventory;

import com.fancyinnovations.fancycore.api.inventory.Kit;
import com.fancyinnovations.fancycore.api.inventory.KitsService;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import org.jetbrains.annotations.NotNull;

public class ListKitsCMD extends CommandBase {

    public ListKitsCMD() {
        super("listkits", "Lists all available kits");
        addAliases("kits");
        requirePermission("fancycore.commands.listkits");
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

        fp.sendMessage("Available Kits:");
        for (Kit kit : KitsService.get().getAllKits()) {
            if (!PermissionsModule.get().hasPermission(fp.getData().getUUID(), "fancycore.kits." + kit.name())) {
                continue;
            }

            fp.sendMessage("- " + kit.name());
        }
    }
}
