package com.fancyinnovations.fancycore.commands.chat.chatroom;

import com.fancyinnovations.fancycore.api.chat.ChatRoom;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.commands.arguments.FancyCoreArgs;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class ChatRoomSwitchCMD extends CommandBase {

    protected final RequiredArg<ChatRoom> chatRoomNameArg = this.withRequiredArg("chatroom", "name of the chatroom to switch to", FancyCoreArgs.CHATROOM);

    protected ChatRoomSwitchCMD() {
        super("switch", "Switch to a chat room. You will be sending messages to this chat room");
        requirePermission("fancycore.commands.chatrooms.switch");
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

        ChatRoom chatRoom = chatRoomNameArg.get(ctx);

        if (!chatRoom.getWatchers().contains(fp)) {
            chatRoom.startWatching(fp);
        }

        fp.switchChatRoom(chatRoom);
        fp.sendMessage("You have switched to chat room " + chatRoom.getName() + ".");
    }
}
