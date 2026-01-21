package com.fancyinnovations.fancycore.commands.moderation;

import com.fancyinnovations.fancycore.api.moderation.PunishmentService;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.commands.arguments.FancyCoreArgs;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WarnCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();
    protected final RequiredArg<FancyPlayer> targetArg = this.withRequiredArg("target", "The player to warn",
            FancyCoreArgs.PLAYER);
    protected final RequiredArg<List<String>> reasonArg = this.withListRequiredArg("reason",
            "The reason for the warning", ArgTypes.STRING);

    public WarnCMD() {
        super("warn", "Warns a player");
        requirePermission("fancycore.commands.warn");
        setAllowsExtraArguments(true);
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
        if (!target.isOnline()) {
            translator.getMessage("moderation.player_offline", fp.getLanguage())
                    .replace("target", target.getData().getUsername())
                    .sendTo(fp);
            return;
        }

        String[] parts = ctx.getInputString().split(" ");
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            reasonBuilder.append(parts[i]);
            if (i < parts.length - 1) {
                reasonBuilder.append(" ");
            }
        }
        String reason = reasonBuilder.toString();

        PunishmentService.get().warnPlayer(target, fp, reason);

        translator.getMessage("moderation.warn.success", fp.getLanguage())
                .replace("target", target.getData().getUsername())
                .replace("reason", reason)
                .sendTo(fp);
    }
}
