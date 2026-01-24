package com.kingc.hytale.duels.mock;

import com.kingc.hytale.duels.api.CommandSource;
import com.kingc.hytale.duels.api.Location;
import com.kingc.hytale.duels.api.PlayerRef;

import java.util.Optional;
import java.util.UUID;

public class MockPlayerRef implements PlayerRef {
    private final UUID id;
    private final String name;
    private Location location;
    private boolean online = true;
    public String lastMessage;

    public MockPlayerRef(UUID id, String name) {
        this.id = id;
        this.name = name;
        this.location = new Location("world", 0, 64, 0, 0, 0);
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void sendMessage(String message) {
        this.lastMessage = message;
        // System.out.println("[MockPlayer:" + name + "] " + message);
    }

    @Override
    public void teleport(Location location) {
        this.location = location;
        // System.out.println("[MockPlayer:" + name + "] Teleported to " + location);
    }

    @Override
    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public Location getLocation() {
        return location;
    }
}
