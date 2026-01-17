package com.fancyinnovations.fancycore.api.events.chat;

import com.fancyinnovations.fancycore.api.chat.ChatRoom;
import com.fancyinnovations.fancycore.api.events.FancyEvent;

/**
 * Event fired when a chat room is cleared.
 */
public class ChatClearedEvent extends FancyEvent {

    private final ChatRoom chatRoom;

    public ChatClearedEvent(ChatRoom chatRoom) {
        super();
        this.chatRoom = chatRoom;
    }

    /**
     * Gets the chat room that was cleared.
     *
     * @return the chat room.
     */
    public ChatRoom getChatRoom() {
        return chatRoom;
    }
}
