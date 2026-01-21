package com.fancyinnovations.fancycore.commands.chat.message;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.commands.arguments.FancyCoreArgs;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class IgnoreCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();
    protected final RequiredArg<FancyPlayer> targetArg = this.withRequiredArg("target", "The player to ignore",
            FancyCoreArgs.PLAYER);

    public IgnoreCMD() {
        super("ignore", "Ignore a player to stop receiving their messages.");
        requirePermission("fancycore.commands.ignore");
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

        FancyPlayer target = targetArg.get(ctx);
        if (target.getData().getUUID().equals(fp.getData().getUUID())) {
            translator.getMessage("error.player.self", fp.getLanguage()).sendTo(fp);
            return;
        }

        if (fp.getData().getIgnoredPlayers().contains(target.getData().getUUID())) {
            translator.getMessage("chat.ignore.already", fp.getLanguage())
                    .replace("player", target.getData().getUsername())
                    .sendTo(fp);
            return;
        }

        fp.getData().addIgnoredPlayer(target.getData().getUUID());
        translator.getMessage("chat.ignore.added", fp.getLanguage())
                .replace("player", target.getData().getUsername())
                .sendTo(fp);
    }
}
