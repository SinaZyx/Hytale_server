package com.fancyinnovations.fancycore.commands.chat.message;

import com.fancyinnovations.fancycore.api.FancyCore;
import com.fancyinnovations.fancycore.api.events.chat.PrivateMessageSentEvent;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ReplyCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();
    protected final RequiredArg<List<String>> messageArg = this.withListRequiredArg("message", "The message to send",
            ArgTypes.STRING);

    public ReplyCMD() {
        super("reply", "Reply to the last player who sent you a private message");
        requirePermission("fancycore.commands.reply");
        addAliases("r");
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            translator.getMessage("error.command.player_only").sendTo(ctx.sender());
            return;
        }

        FancyPlayer sender = FancyPlayerService.get().getByUUID(ctx.sender().getUuid());
        if (sender == null) {
            translator.getMessage("error.player.not_found").sendTo(ctx.sender());
            return;
        }

        FancyPlayer receiver = sender.getReplyTo();
        if (receiver == null) {
            translator.getMessage("chat.reply.no_target", sender.getLanguage()).sendTo(sender);
            return;
        }
        if (!receiver.isOnline()) {
            translator.getMessage("chat.message.player_offline", sender.getLanguage())
                    .replace("player", receiver.getData().getUsername())
                    .sendTo(sender);
            return;
        }

        if (!receiver.getData().isPrivateMessagesEnabled()) {
            translator.getMessage("chat.message.disabled", sender.getLanguage())
                    .replace("player", receiver.getData().getUsername())
                    .sendTo(sender);
            return;
        }

        if (receiver.getData().getIgnoredPlayers().contains(sender.getData().getUUID())) {
            translator.getMessage("chat.message.ignored", sender.getLanguage())
                    .replace("player", receiver.getData().getUsername())
                    .sendTo(sender);
            return;
        }

        String message = String.join(" ", messageArg.get(ctx));

        if (!new PrivateMessageSentEvent(sender, receiver, message).fire()) {
            return;
        }

        String parsedMessage = FancyCore.get().getConfig().getPrivateMessageFormat()
                .replace("%sender%", sender.getData().getUsername())
                .replace("%receiver%", receiver.getData().getUsername())
                .replace("%message%", message);

        receiver.sendMessage(parsedMessage);
        sender.sendMessage(parsedMessage);

        sender.setReplyTo(receiver);
        receiver.setReplyTo(sender);
    }
}
