package com.fancyinnovations.fancycore.api.events.chat;

import com.fancyinnovations.fancycore.api.chat.ChatRoom;
import com.fancyinnovations.fancycore.api.events.player.PlayerEvent;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;

/**
 * Event fired when a player starts watching a chat room.
 */
public class PlayerWatchedChatRoomEvent extends PlayerEvent {

    private final ChatRoom chatRoom;

    public PlayerWatchedChatRoomEvent(FancyPlayer player, ChatRoom chatRoom) {
        super(player);
        this.chatRoom = chatRoom;
    }

    /**
     * Gets the chat room being watched.
     *
     * @return the chat room.
     */
    public ChatRoom getChatRoom() {
        return chatRoom;
    }
}
