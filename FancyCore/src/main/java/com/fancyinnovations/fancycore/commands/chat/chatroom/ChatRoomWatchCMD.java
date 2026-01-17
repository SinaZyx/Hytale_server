package com.fancyinnovations.fancycore.commands.chat.chatroom;

import com.fancyinnovations.fancycore.api.chat.ChatRoom;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.commands.arguments.FancyCoreArgs;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import org.jetbrains.annotations.NotNull;

public class ChatRoomWatchCMD extends CommandBase {

    protected final RequiredArg<ChatRoom> chatRoomNameArg = this.withRequiredArg("chatroom", "name of the chatroom to start watching", FancyCoreArgs.CHATROOM);

    protected ChatRoomWatchCMD() {
        super("watch", "Start watching a chat room. You will receive messages from this chat room");
        requirePermission("fancycore.commands.chatrooms.watch");
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

        if (!PermissionsModule.get().hasPermission(fp.getData().getUUID(), "fancycore.chatrooms." + chatRoom.getName())) {
            fp.sendMessage("You do not have permission to watch this chat room.");
            return;
        }

        if (chatRoom.getWatchers().contains(fp)) {
            fp.sendMessage("You are already watching chat room " + chatRoom.getName() + ".");
            return;
        }

        chatRoom.startWatching(fp);

        fp.sendMessage("You are now watching chat room " + chatRoom.getName() + ".");
    }
}
