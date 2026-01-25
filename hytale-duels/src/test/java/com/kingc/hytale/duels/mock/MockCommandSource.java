package com.kingc.hytale.duels.mock;

import com.kingc.hytale.duels.api.CommandSource;
import com.kingc.hytale.duels.api.PlayerRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MockCommandSource implements CommandSource {
    private final UUID id;
    private final String name;
    public final List<String> messages = new ArrayList<>();

    public MockCommandSource(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public void sendMessage(String message) {
        messages.add(message);
    }

    @Override
    public Optional<UUID> playerId() {
        return Optional.ofNullable(id);
    }

    @Override
    public Optional<PlayerRef> player() {
        if (id == null) return Optional.empty();
        return Optional.of(new MockPlayerRef(id, name));
    }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }

    public String getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }
}
