package com.fancyinnovations.fancycore.commands.chat;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class ChatColorCMD extends AbstractCommandCollection {

    public ChatColorCMD() {
        super("chatcolor", "Manage your chat color");
        requirePermission("fancycore.commands.chatcolor");

        addSubCommand(new ChatColorSetCMD());
    }
}
