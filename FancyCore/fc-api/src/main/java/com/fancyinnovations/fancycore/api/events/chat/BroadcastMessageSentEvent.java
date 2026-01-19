package com.fancyinnovations.fancycore.api.events.chat;

import com.fancyinnovations.fancycore.api.chat.ChatRoom;
import com.fancyinnovations.fancycore.api.events.FancyEvent;

/**
 * Event fired when a broadcast message is sent to a chat room.
 */
public class BroadcastMessageSentEvent extends FancyEvent {

    private final ChatRoom chatRoom;
    private final String rawMessage;
    private final String parsedMessage;

    public BroadcastMessageSentEvent(ChatRoom chatRoom, String rawMessage, String parsedMessage) {
        super();
        this.chatRoom = chatRoom;
        this.rawMessage = rawMessage;
        this.parsedMessage = parsedMessage;
    }

    /**
     * Gets the raw message that was sent.
     *
     * @return the chat room.
     */
    public ChatRoom getChatRoom() {
        return chatRoom;
    }

    /**
     * Gets the raw message that was sent.
     *
     * @return the raw message.
     */
    public String getRawMessage() {
        return rawMessage;
    }

    /**
     * Gets the parsed message that was sent.
     *
     * @return the parsed message.
     */
    public String getParsedMessage() {
        return parsedMessage;
    }
}
