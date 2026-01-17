package com.fancyinnovations.fancycore.translations;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;

public class Message {

    private final String key;
    private final String raw;
    private String parsed;

    public Message(String key, String message) {
        this.key = key;
        this.raw = message;
        this.parsed = message;
    }

    public Message replace(String placeholder, String replacement) {
        this.parsed = this.parsed
                .replace("{" + placeholder + "}", replacement)
                .replace("%" + placeholder + "%", replacement);

        return this;
    }

    public void sendTo(FancyPlayer player) {
        if (!player.isOnline()) {
            return;
        }

        player.sendMessage(this.parsed);
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
}
