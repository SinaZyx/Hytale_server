package com.fancyinnovations.fancycore.commands.chat;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class ChatColorSetCMD extends CommandBase {

    protected final RequiredArg<String> colorArg = this.withRequiredArg("color", "the color code to set", ArgTypes.STRING);

    public ChatColorSetCMD() {
        super("set", "Set your chat color");
        requirePermission("fancycore.commands.chatcolor.set");
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be executed by a player."));
            return;
        }

        FancyPlayer fp = FancyPlayerService.get().getByUUID(ctx.sender().getUuid());
        if (fp == null) {
            ctx.sendMessage(Message.raw("FancyPlayer not found."));
            return;
        }

        String colorCode = colorArg.get(ctx);
        if (!colorCode.startsWith("&")) {
            ctx.sendMessage(Message.raw("Invalid color code. It should start with '&'."));
            return;
        }
        if (colorCode.length() != 2) {
            ctx.sendMessage(Message.raw("Invalid color code. It should be in the format '&<code>'. Example: '&a' for green."));
            return;
        }

        fp.getData().setChatColor(colorCode);

        fp.sendMessage("Your chat color has been set to " + colorCode + "this &rcolor.");
    }
}
