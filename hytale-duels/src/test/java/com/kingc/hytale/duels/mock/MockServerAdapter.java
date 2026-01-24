package com.kingc.hytale.duels.mock;

import com.kingc.hytale.duels.api.ItemStack;
import com.kingc.hytale.duels.api.PlayerRef;
import com.kingc.hytale.duels.api.ServerAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MockServerAdapter implements ServerAdapter {
    private final Map<UUID, PlayerRef> players = new ConcurrentHashMap<>();
    private long currentTime = System.currentTimeMillis();

    public void addPlayer(PlayerRef player) {
        players.put(player.id(), player);
    }

    @Override
    public long nowEpochMs() {
        return currentTime;
    }

    public void setCurrentTime(long time) {
        this.currentTime = time;
    }

    @Override
    public Optional<PlayerRef> getPlayer(UUID id) {
        return Optional.ofNullable(players.get(id));
    }

    @Override
    public Optional<PlayerRef> getPlayerByName(String name) {
        return players.values().stream()
                .filter(p -> p.name().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public void giveItems(PlayerRef player, ItemStack... items) {
        // Mock inventory manipulation
    }

    @Override
    public void setArmor(PlayerRef player, ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
        // Mock armor
    }

    @Override
    public void teleportToLobby(PlayerRef player) {
        // Mock lobby TP
        player.teleport(new com.kingc.hytale.duels.api.Location("lobby", 0, 100, 0, 0, 0));
    }

    @Override
    public void clearInventory(PlayerRef player) {
        // Mock clear
    }

    @Override
    public void applyEffect(PlayerRef player, String effectType, int amplifier, int durationTicks) {
        // Mock effect
    }

    @Override
    public void clearEffects(PlayerRef player) {
        // Mock clear effects
    }

    @Override
    public void showTitle(PlayerRef player, String title, String subtitle, String color, float fadeIn, float stay, float fadeOut) {
        // System.out.println("[Title:" + player.name() + "] " + title + " / " + subtitle);
    }

    @Override
    public List<ItemStack> getInventory(PlayerRef player) {
        return Collections.emptyList();
    }

    @Override
    public ItemStack[] getArmor(PlayerRef player) {
        return new ItemStack[4];
    }
}
