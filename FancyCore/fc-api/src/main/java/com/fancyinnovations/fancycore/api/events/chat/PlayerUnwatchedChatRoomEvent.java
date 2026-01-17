package com.fancyinnovations.fancycore.api.events.chat;

import com.fancyinnovations.fancycore.api.chat.ChatRoom;
import com.fancyinnovations.fancycore.api.events.player.PlayerEvent;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;

/**
 * Event fired when a player stops watching a chat room.
 */
public class PlayerUnwatchedChatRoomEvent extends PlayerEvent {

    private final ChatRoom chatRoom;

    public PlayerUnwatchedChatRoomEvent(FancyPlayer player, ChatRoom chatRoom) {
        super(player);
        this.chatRoom = chatRoom;
    }

    /**
     * Gets the chat room being unwatched.
     *
     * @return the chat room.
     */
    public ChatRoom getChatRoom() {
        return chatRoom;
    }
}
