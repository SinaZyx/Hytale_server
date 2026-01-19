package com.fancyinnovations.fancycore.commands.chat;

import com.fancyinnovations.fancycore.commands.FancyCommandBase;

public class ChatColorCMD extends FancyCommandBase {

    public ChatColorCMD() {
        super("chatcolor", "Manage your chat color");
        // requirePermission("fancycore.commands.chatcolor"); // Removed to allow
        // localized check in subcommands or if we want to add check here later

        addSubCommand(new ChatColorSetCMD());
    }
}
