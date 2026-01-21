package com.kingc.hytale.factions.translations;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;

public class FactionMessage {

    private final String key;
    private final String raw;
    private String parsed;

    public FactionMessage(String key, String message) {
        this.key = key;
        this.raw = message;
        this.parsed = message;
    }

    public FactionMessage replace(String placeholder, String replacement) {
        this.parsed = this.parsed
                .replace("{" + placeholder + "}", replacement)
                .replace("%" + placeholder + "%", replacement);
        return this;
    }

    public void sendTo(CommandSender sender) {
        sender.sendMessage(Message.raw(this.parsed));
    }

    public String getKey() {
        return key;
    }

    public String getRawMessage() {
        return raw;
    }

    public String getParsedMessage() {
        return parsed;
    }

    @Override
    public String toString() {
        return parsed;
    }
}
