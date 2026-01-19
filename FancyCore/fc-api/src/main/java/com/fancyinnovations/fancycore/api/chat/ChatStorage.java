package com.fancyinnovations.fancycore.api.chat;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public interface ChatStorage {

    @ApiStatus.Internal
    void setChatRoom(ChatRoom room);

    @ApiStatus.Internal
    ChatRoom getChatRoom(String room);

    @ApiStatus.Internal
    void deleteChatRoom(String room);

    @ApiStatus.Internal
    List<ChatRoom> getAllChatRooms();

}
