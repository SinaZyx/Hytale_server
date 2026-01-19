package com.fancyinnovations.fancycore.chat.service;

import com.fancyinnovations.fancycore.api.chat.ChatRoom;
import com.fancyinnovations.fancycore.api.chat.ChatService;
import com.fancyinnovations.fancycore.api.chat.ChatStorage;
import com.fancyinnovations.fancycore.chat.ChatRoomImpl;
import com.fancyinnovations.fancycore.chat.storage.fake.ChatFakeStorage;
import com.fancyinnovations.fancycore.chat.storage.json.ChatJsonStorage;

import java.util.List;

public class ChatServiceImpl implements ChatService {

    private final ChatStorage storage;
    private final ChatStorage cache;

    public ChatServiceImpl() {
        this.storage = new ChatJsonStorage();
        this.cache = new ChatFakeStorage();
        load();
    }

    private void load() {
        for (ChatRoom room : storage.getAllChatRooms()) {
            cache.setChatRoom(room);
        }
    }

    @Override
    public ChatRoom createChatRoom(String name) {
        ChatRoom room = new ChatRoomImpl(name);

        storage.setChatRoom(room);
        cache.setChatRoom(room);
        return room;
    }

    @Override
    public ChatRoom getChatRoom(String room) {
        return cache.getChatRoom(room);
    }

    @Override
    public void deleteChatRoom(String room) {
        storage.deleteChatRoom(room);
        cache.deleteChatRoom(room);
    }

    @Override
    public List<ChatRoom> getAllChatRooms() {
        return cache.getAllChatRooms();
    }
}
