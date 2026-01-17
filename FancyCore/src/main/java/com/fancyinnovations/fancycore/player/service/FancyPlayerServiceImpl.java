package com.fancyinnovations.fancycore.player.service;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerData;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.api.player.FancyPlayerStorage;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.player.FancyPlayerImpl;
import com.fancyinnovations.fancycore.player.storage.json.JsonFancyPlayer;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FancyPlayerServiceImpl implements FancyPlayerService {

    private final Map<UUID, FancyPlayer> cache;
    private final FancyPlayerStorage storage;

    public FancyPlayerServiceImpl() {
        this.cache = new ConcurrentHashMap<>();
        this.storage = FancyCorePlugin.get().getPlayerStorage();
    }

    @Override
    public FancyPlayer getByUUID(UUID uuid) {
        // Check cache
        FancyPlayer cached = cache.get(uuid);
        if (cached != null) {
            // If cached player doesn't have PlayerRef, check if they're actually online
            if (cached.getPlayer() == null) {
                PlayerRef playerRef = Universe.get().getPlayer(uuid);
                if (playerRef != null && playerRef.isValid()) {
                    cached.setPlayer(playerRef);
                }
            }
            return cached;
        }

        // Try to get from storage
        FancyPlayer fromStorage = tryToGetFromStorage(uuid);
        if (fromStorage != null) {
            // Check if player is actually online and update PlayerRef
            PlayerRef playerRef = Universe.get().getPlayer(uuid);
            if (playerRef != null && playerRef.isValid()) {
                fromStorage.setPlayer(playerRef);
            }
        }

        return fromStorage;
    }

    @Override
    public FancyPlayer getByUsername(String username) {
        // Check cache
        for (FancyPlayer fp : cache.values()) {
            if (fp.getData().getUsername().equalsIgnoreCase(username)) {
                // If cached player doesn't have PlayerRef, check if they're actually online
                if (fp.getPlayer() == null) {
                    PlayerRef playerRef = Universe.get().getPlayerByUsername(username, NameMatching.EXACT);
                    if (playerRef != null && playerRef.isValid()) {
                        fp.setPlayer(playerRef);
                    }
                }
                return fp;
            }
        }

        // Try to get from storage
        FancyPlayer fromStorage = tryToGetFromStorage(username);
        if (fromStorage != null) {
            // Check if player is actually online and update PlayerRef
            PlayerRef playerRef = Universe.get().getPlayerByUsername(username, NameMatching.EXACT);
            if (playerRef != null && playerRef.isValid()) {
                fromStorage.setPlayer(playerRef);
            }
        }

        return fromStorage;
    }

    @Override
    public List<FancyPlayer> getOnlinePlayers() {
        List<FancyPlayer> onlinePlayers = new ArrayList<>();

        for (PlayerRef player : Universe.get().getPlayers()) {
            FancyPlayer fancyPlayer = getByUUID(player.getUuid());
            if (fancyPlayer != null) {
                onlinePlayers.add(fancyPlayer);
            }
        }

        return onlinePlayers;
    }

    @Override
    public FancyPlayerData fromJson(String json) {
        return FancyCorePlugin.GSON.fromJson(json, JsonFancyPlayer.class).toFancyPlayer();
    }

    @ApiStatus.Internal
    public List<FancyPlayer> getAllCached() {
        return new ArrayList<>(cache.values());
    }

    public void addPlayerToCache(FancyPlayer player) {
        cache.put(player.getData().getUUID(), player);
    }

    public FancyPlayer tryToGetFromStorage(UUID uuid) {
        try {
            FancyPlayerData data = storage.loadPlayer(uuid);
            if (data == null) {
                return null;
            }

            FancyPlayer fancyPlayer = new FancyPlayerImpl(data);
            addPlayerToCache(fancyPlayer);
            return fancyPlayer;
        } catch (Exception e) {
            return null;
        }
    }

    public FancyPlayer tryToGetFromStorage(String username) {
        for (FancyPlayerData allPlayer : storage.loadAllPlayers()) {
            if (allPlayer.getUsername().equalsIgnoreCase(username)) {
                FancyPlayer fancyPlayer = new FancyPlayerImpl(allPlayer);
                addPlayerToCache(fancyPlayer);
                return fancyPlayer;
            }
        }
        return null;

//        try {
//            FancyPlayerData data = storage.loadPlayerByUsername(username);
//            if (data == null) {
//                return null;
//            }
//
//            FancyPlayer fancyPlayer = new FancyPlayerImpl(data);
//            addPlayerToCache(fancyPlayer);
//            return fancyPlayer;
//        } catch (Exception e) {
//            return null;
//        }
    }

    public void removePlayerFromCache(UUID uuid) {
        cache.remove(uuid);
    }

}
