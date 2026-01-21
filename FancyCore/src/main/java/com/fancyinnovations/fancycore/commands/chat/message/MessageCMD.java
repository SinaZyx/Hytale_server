package com.fancyinnovations.fancycore.commands.chat.message;

import com.fancyinnovations.fancycore.api.FancyCore;
import com.fancyinnovations.fancycore.api.events.chat.PrivateMessageSentEvent;
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

public class MessageCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();
    protected final RequiredArg<FancyPlayer> receiverArg = this.withRequiredArg("receiver",
            "The player to send the message to", FancyCoreArgs.PLAYER);
    protected final RequiredArg<List<String>> messageArg = this.withListRequiredArg("message", "The message to send",
            ArgTypes.STRING);

    public MessageCMD() {
        super("message", "Send a private message to another player");
        addAliases("msg", "dm", "tell", "whisper");
        requirePermission("fancycore.commands.message");
        setAllowsExtraArguments(true);
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

        FancyPlayer receiver = receiverArg.get(ctx);
        if (!receiver.isOnline()) {
            translator.getMessage("chat.message.player_offline", sender.getLanguage())
                    .replace("player", receiver.getData().getUsername())
                    .sendTo(sender);
            return;
        }

        if (receiver.getData().getUUID().equals(sender.getData().getUUID())) {
            translator.getMessage("chat.message.cannot_self", sender.getLanguage()).sendTo(sender);
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

        String[] parts = ctx.getInputString().split(" ");
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            messageBuilder.append(parts[i]);
            if (i < parts.length - 1) {
                messageBuilder.append(" ");
            }
        }
        String message = messageBuilder.toString();

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
