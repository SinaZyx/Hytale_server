package com.kingc.hytale.duels.translations;

import com.kingc.hytale.duels.api.CommandSource;

public class DuelsMessage {
    private final String key;
    private final String rawMessage;

    public DuelsMessage(String key, String rawMessage) {
        this.key = key;
        this.rawMessage = rawMessage;
    }

    public DuelsMessage with(String placeholder, Object value) {
        String val = value == null ? "null" : value.toString();
        return new DuelsMessage(key, rawMessage.replace("{" + placeholder + "}", val));
    }

    public void send(CommandSource source) {
        if (source != null) {
            source.sendMessage(rawMessage);
        }
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return rawMessage;
    }
}
