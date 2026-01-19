package com.fancyinnovations.fancycore.commands.player;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.commands.FancyLeafCommandBase;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import org.jetbrains.annotations.NotNull;

public class LanguageCMD extends FancyLeafCommandBase {

    private final RequiredArg<String> languageArg = this.withRequiredArg("language",
            "The language to switch to (en, fr)", ArgTypes.STRING);

    public LanguageCMD() {
        super("lang", "Change your language");
        addAliases("language");
    }

    @Override
    public void executeSync(@NotNull CommandContext ctx) {
        if (!checkPermission(ctx, "fancycore.command.language")) {
            return;
        }

        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command is only available for players."));
            return;
        }

        FancyPlayer player = FancyCorePlugin.get().getPlayerService().getByUUID(ctx.sender().getUuid());
        if (player == null) {
            return;
        }

        // RequiredArg is always provided when executeSync is called

        String newLang = languageArg.get(ctx).toLowerCase();
        if (!newLang.equals("en") && !newLang.equals("fr")) {
            FancyCorePlugin.get().getTranslationService()
                    .getMessage("error.arg.invalid_language", player.getLanguage())
                    .sendTo(player);
            return;
        }

        player.setLanguage(newLang);
        FancyCorePlugin.get().getTranslationService()
                .getMessage("fancycore.language.changed", newLang)
                .replace("lang", newLang)
                .sendTo(player);
    }
}
