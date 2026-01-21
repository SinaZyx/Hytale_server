package com.fancyinnovations.fancycore.commands.chat.message;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class ToggleMessagesCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();

    public ToggleMessagesCMD() {
        super("togglemessages", "Toggle receiving private messages from all players.");
        addAliases("toggledms");
        requirePermission("fancycore.commands.togglemessages");
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

        fp.getData().setPrivateMessagesEnabled(!fp.getData().isPrivateMessagesEnabled());

        String key = fp.getData().isPrivateMessagesEnabled() ? "chat.toggle.enabled" : "chat.toggle.disabled";
        translator.getMessage(key, fp.getLanguage()).sendTo(fp);
    }
}
