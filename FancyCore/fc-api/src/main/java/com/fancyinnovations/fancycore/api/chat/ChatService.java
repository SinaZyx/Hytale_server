package com.fancyinnovations.fancycore.api.chat;

import com.fancyinnovations.fancycore.api.FancyCore;

import java.util.List;

public interface ChatService {

    static ChatService get() {
        return FancyCore.get().getChatService();
    }

    ChatRoom createChatRoom(String name);

    ChatRoom getChatRoom(String room);

    void deleteChatRoom(String room);

    List<ChatRoom> getAllChatRooms();

}
