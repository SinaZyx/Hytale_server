package com.fancyinnovations.fancycore.api.events.chat;

import com.fancyinnovations.fancycore.api.chat.ChatRoom;
import com.fancyinnovations.fancycore.api.events.player.PlayerEvent;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;

/**
 * Event fired when a player switches chat rooms.
 */
public class PlayerSwitchedChatRoomEvent extends PlayerEvent {

    private final ChatRoom oldChatRoom;
    private final ChatRoom newChatRoom;

    public PlayerSwitchedChatRoomEvent(FancyPlayer player, ChatRoom oldChatRoom, ChatRoom newChatRoom) {
        super(player);
        this.oldChatRoom = oldChatRoom;
        this.newChatRoom = newChatRoom;
    }

    /**
     * Gets the old chat room before the switch.
     *
     * @return the old chat room.
     */
    public ChatRoom getOldChatRoom() {
        return oldChatRoom;
    }

    /**
     * Gets the new chat room after the switch.
     *
     * @return the new chat room.
     */
    public ChatRoom getNewChatRoom() {
        return newChatRoom;
    }
}
