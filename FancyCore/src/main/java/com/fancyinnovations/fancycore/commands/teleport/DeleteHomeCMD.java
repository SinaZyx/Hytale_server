package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DeleteHomeCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();
    protected final RequiredArg<String> nameArg = this.withRequiredArg("", "Home name", ArgTypes.STRING);

    public DeleteHomeCMD() {
        super("deletehome", "Deletes your home point with the specified name");
        addAliases("delhome");
        requirePermission("fancycore.commands.deletehome");
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

        String homeName = nameArg.get(ctx);
        if (homeName == null || homeName.trim().isEmpty()) {
            translator.getMessage("teleport.home.name_empty", fp.getLanguage())
                .sendTo(fp);
            return;
        }

        // Get homes map
        Map<String, Object> customData = fp.getData().getCustomData();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> homes = (Map<String, Map<String, Object>>) customData.get("homes");

        if (homes == null || !homes.containsKey(homeName)) {
            translator.getMessage("teleport.home.not_found", fp.getLanguage())
                .replace("name", homeName)
                .sendTo(fp);
            return;
        }

        // Delete home
        homes.remove(homeName);
        fp.getData().setCustomData("homes", homes);

        // Send success message
        translator.getMessage("teleport.home.deleted", fp.getLanguage())
            .replace("name", homeName)
            .sendTo(fp);
    }
}
