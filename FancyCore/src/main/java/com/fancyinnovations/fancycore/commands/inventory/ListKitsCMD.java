package com.fancyinnovations.fancycore.commands.inventory;

import com.fancyinnovations.fancycore.api.inventory.Kit;
import com.fancyinnovations.fancycore.api.inventory.KitsService;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import org.jetbrains.annotations.NotNull;

public class ListKitsCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();

    public ListKitsCMD() {
        super("listkits", "Lists all available kits");
        addAliases("kits");
        requirePermission("fancycore.commands.listkits");
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

        var kits = KitsService.get().getAllKits().stream()
                .filter(kit -> PermissionsModule.get().hasPermission(fp.getData().getUUID(),
                        "fancycore.kits." + kit.name()))
                .toList();

        if (kits.isEmpty()) {
            translator.getMessage("kit.list.empty", fp.getLanguage()).sendTo(fp);
            return;
        }

        translator.getMessage("kit.list.header", fp.getLanguage()).sendTo(fp);
        for (Kit kit : kits) {
            translator.getMessage("kit.list.entry", fp.getLanguage())
                    .replace("name", kit.name())
                    .sendTo(fp);
        }
    }
}
