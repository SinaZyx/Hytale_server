package com.fancyinnovations.fancycore.commands.inventory;

import com.fancyinnovations.fancycore.api.inventory.Kit;
import com.fancyinnovations.fancycore.api.inventory.KitsService;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.commands.arguments.FancyCoreArgs;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class DeleteKitCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();
    protected final RequiredArg<Kit> kitArg = this.withRequiredArg("kit", "the name of the kit", FancyCoreArgs.KIT);

    public DeleteKitCMD() {
        super("deletekit", "Deletes the specified kit");
        addAliases("delkit");
        requirePermission("fancycore.commands.deletekit");
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            translator.getMessage("error.command.player_only").sendTo(ctx.sender());
            return;
        }

        FancyPlayer fp = FancyPlayerService.get().getByUUID(ctx.sender().getUuid());
        if (fp == null) {
            translator.getMessage("error.player.not_found").sendTo(ctx.sender());
            return;
        }

        Kit kit = kitArg.get(ctx);
        KitsService.get().deleteKit(kit);

        translator.getMessage("kit.deleted", fp.getLanguage())
                .replace("name", kit.name())
                .sendTo(fp);
    }
}
