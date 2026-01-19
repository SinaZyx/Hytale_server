package com.fancyinnovations.fancycore.player.storage.fake;

import com.fancyinnovations.fancycore.api.player.FancyPlayerData;
import com.fancyinnovations.fancycore.api.player.FancyPlayerStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FancyPlayerFakeStorage implements FancyPlayerStorage {

    private final Map<UUID, FancyPlayerData> players;

    public FancyPlayerFakeStorage() {
        players = new ConcurrentHashMap<>();
    }

    @Override
    public void savePlayer(FancyPlayerData player) {
        players.put(player.getUUID(), player);
    }

    @Override
    public FancyPlayerData loadPlayer(UUID uuid) {
        return players.get(uuid);
    }

    @Override
    public FancyPlayerData loadPlayerByUsername(String username) {
        for (FancyPlayerData playerData : players.values()) {
            if (playerData.getUsername().equalsIgnoreCase(username)) {
                return playerData;
            }
        }
        return null;
    }

    @Override
    public List<FancyPlayerData> loadAllPlayers() {
        return new ArrayList<>(players.values());
    }

    @Override
    public void deletePlayer(UUID uuid) {
        players.remove(uuid);
    }

    @Override
    public int countPlayers() {
        return players.size();
    }
}
