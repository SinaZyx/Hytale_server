package com.fancyinnovations.fancycore.commands;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import javax.annotation.Nullable;

public abstract class FancyCommandBase extends AbstractCommandCollection {

    protected final TranslationService translator = FancyCorePlugin.get().getTranslationService();

    protected FancyCommandBase(String name, String description) {
        super(name, description);
    }

    /**
     * Sends the "player only" error message to the command sender.
     */
    protected void sendPlayerOnlyError(CommandContext ctx) {
        translator.getMessage("error.command.player_only").sendTo(ctx.sender());
    }

    /**
     * Requires a FancyPlayer from the command context.
     * Sends error message if player not found.
     * @return FancyPlayer or null if not found
     */
    @Nullable
    protected FancyPlayer requireFancyPlayer(CommandContext ctx) {
        FancyPlayer fp = FancyCorePlugin.get().getPlayerService().getByUUID(ctx.sender().getUuid());
        if (fp == null) {
            translator.getMessage("error.player.not_found").sendTo(ctx.sender());
        }
        return fp;
    }

    /**
     * Sends a translated success message to a player with placeholder replacements.
     * @param key Translation key
     * @param player Target player
     * @param replacements Key-value pairs for placeholder replacement (key1, value1, key2, value2, ...)
     */
    protected void sendSuccess(String key, FancyPlayer player, Object... replacements) {
        com.fancyinnovations.fancycore.translations.Message msg = translator.getMessage(key, player.getLanguage());
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                msg = msg.replace((String) replacements[i], String.valueOf(replacements[i + 1]));
            }
        }
        msg.sendTo(player);
    }

    /**
     * Sends a translated error message to a player with placeholder replacements.
     * @param key Translation key
     * @param player Target player
     * @param replacements Key-value pairs for placeholder replacement (key1, value1, key2, value2, ...)
     */
    protected void sendError(String key, FancyPlayer player, Object... replacements) {
        com.fancyinnovations.fancycore.translations.Message msg = translator.getMessage(key, player.getLanguage());
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                msg = msg.replace((String) replacements[i], String.valueOf(replacements[i + 1]));
            }
        }
        msg.sendTo(player);
    }

    /**
     * Sends a translated message to command context (works for both players and console).
     * @param ctx Command context
     * @param key Translation key
     * @param replacements Key-value pairs for placeholder replacement
     */
    protected void sendMessage(CommandContext ctx, String key, Object... replacements) {
        String language = "en"; // Default to English for console

        if (ctx.isPlayer()) {
            FancyPlayer fp = FancyCorePlugin.get().getPlayerService().getByUUID(ctx.sender().getUuid());
            if (fp != null) {
                language = fp.getLanguage();
            }
        }

        com.fancyinnovations.fancycore.translations.Message msg = translator.getMessage(key, language);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                msg = msg.replace((String) replacements[i], String.valueOf(replacements[i + 1]));
            }
        }
        msg.sendTo(ctx.sender());
    }

    /**
     * Gets a translated message for a specific language.
     * @param key Translation key
     * @param language Language code
     * @return Translated message
     */
    protected com.fancyinnovations.fancycore.translations.Message getMessage(String key, String language) {
        return translator.getMessage(key, language);
    }

    /**
     * Gets a translated message for a player's language.
     * @param key Translation key
     * @param player Player
     * @return Translated message
     */
    protected com.fancyinnovations.fancycore.translations.Message getMessage(String key, FancyPlayer player) {
        return translator.getMessage(key, player.getLanguage());
    }

    protected boolean checkPermission(CommandContext ctx, String permission) {
        if (ctx.sender().hasPermission(permission)) {
            return true;
        }

        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("You do not have permission to use this command."));
            return false;
        }

        FancyPlayer player = FancyCorePlugin.get().getPlayerService().getByUUID(ctx.sender().getUuid());
        if (player == null) {
            ctx.sendMessage(Message.raw("You do not have permission to use this command."));
            return false;
        }

        String language = player.getLanguage();
        translator.getMessage("error.permission", language).sendTo(player);

        return false;
    }
}
