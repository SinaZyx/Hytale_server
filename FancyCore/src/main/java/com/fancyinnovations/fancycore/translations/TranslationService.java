package com.fancyinnovations.fancycore.translations;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TranslationService {

    private final Map<String, Message> messages;

    public TranslationService(Map<String, Message> messages) {
        this.messages = messages;
    }

    public TranslationService() {
        this.messages = new ConcurrentHashMap<>();
    }

    public TranslationService addMessage(Message message) {
        this.messages.put(message.getKey(), message);
        return this;
    }

    public TranslationService addMessage(String key, String message) {
        this.messages.put(key, new Message(key, message));
        return this;
    }

    public Message getMessage(String key) {
        Message message = this.messages.get(key);
        if (message == null) {
            // return a default message if the key does not exist
            return new Message(key, "Missing translation for key: " + key);
        }

        // return a copy to prevent external modification
        return new Message(message.getKey(), message.getRawMessage());
    }
}
