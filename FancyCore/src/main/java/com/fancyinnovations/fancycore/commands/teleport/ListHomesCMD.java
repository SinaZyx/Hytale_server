package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.api.player.Home;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class ListHomesCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();

    public ListHomesCMD() {
        super("listhomes", "Lists all your home points");
        addAliases("homes");
        requirePermission("fancycore.commands.listhomes");
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

        translator.getMessage("teleport.list.homes.header", fp.getLanguage()).sendTo(fp);
        for (Home home : fp.getData().getHomes()) {
            translator.getMessage("teleport.list.homes.entry", fp.getLanguage())
                .replace("name", home.name())
                .replace("world", home.location().worldName())
                .replace("x", String.valueOf((int)home.location().x()))
                .replace("y", String.valueOf((int)home.location().y()))
                .replace("z", String.valueOf((int)home.location().z()))
                .sendTo(fp);
        }
    }
}
