package com.fancyinnovations.fancycore.chat.storage.json;

import com.fancyinnovations.fancycore.api.chat.ChatRoom;
import com.fancyinnovations.fancycore.chat.ChatRoomImpl;
import com.google.gson.annotations.SerializedName;

public record JsonChatRoom(
        String name,
        @SerializedName("is_muted") boolean isMuted,
        long cooldown
) {

    public static JsonChatRoom from(ChatRoom room) {
        return new JsonChatRoom(
                room.getName(),
                room.isMuted(),
                room.getCooldown()
        );
    }

    public ChatRoom toChatRoom() {
        return new ChatRoomImpl(
                name,
                isMuted,
                cooldown
        );
    }
}
