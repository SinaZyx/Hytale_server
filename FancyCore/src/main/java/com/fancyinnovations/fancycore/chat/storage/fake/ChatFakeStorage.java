package com.fancyinnovations.fancycore.chat.storage.fake;

import com.fancyinnovations.fancycore.api.chat.ChatRoom;
import com.fancyinnovations.fancycore.api.chat.ChatStorage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatFakeStorage implements ChatStorage {

    private final Map<String, ChatRoom> rooms;

    public ChatFakeStorage() {
        this.rooms = new ConcurrentHashMap<>();
    }

    @Override
    public void setChatRoom(ChatRoom room) {
        rooms.put(room.getName(), room);
    }

    @Override
    public ChatRoom getChatRoom(String room) {
        return rooms.get(room);
    }

    @Override
    public void deleteChatRoom(String room) {
        rooms.remove(room);
    }

    @Override
    public List<ChatRoom> getAllChatRooms() {
        return List.copyOf(rooms.values());
    }
}
