package com.fancyinnovations.fancycore.commands.player;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.commands.FancyLeafCommandBase;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;

public class LanguageCMD extends FancyLeafCommandBase {

    private final RequiredArg<String> languageArg = this.withRequiredArg("language",
            "The language to switch to (ex: en)", ArgTypes.STRING);

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
            sendPlayerOnlyError(ctx);
            return;
        }

        FancyPlayer player = requireFancyPlayer(ctx);
        if (player == null) {
            return;
        }

        // RequiredArg is always provided when executeSync is called
        String newLang = languageArg.get(ctx).toLowerCase(Locale.ROOT);
        Set<String> availableLanguages = translator.getAvailableLanguages();
        if (!availableLanguages.contains(newLang)) {
            sendError("error.arg.invalid_language", player,
                "languages", String.join(", ", availableLanguages));
            return;
        }

        player.setLanguage(newLang);
        sendSuccess("fancycore.language.changed", player, "lang", newLang);
    }
}
