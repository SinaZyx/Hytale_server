package com.kingc.hytale.duels.api;

import java.util.Optional;
import java.util.UUID;

public interface ServerAdapter {
    long nowEpochMs();
    Optional<PlayerRef> getPlayer(UUID id);
    Optional<PlayerRef> getPlayerByName(String name);
    void giveItems(PlayerRef player, ItemStack... items);
    void setArmor(PlayerRef player, ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots);

    /**
     * Teleports the player to the server lobby/spawn.
     */
    void teleportToLobby(PlayerRef player);
    void clearInventory(PlayerRef player);
    void applyEffect(PlayerRef player, String effectType, int amplifier, int durationTicks);
    void clearEffects(PlayerRef player);

    // Visual effects
    void showTitle(PlayerRef player, String title, String subtitle, String color, float fadeIn, float stay, float fadeOut);

    default void showTitle(PlayerRef player, String title, String subtitle, String color) {
        showTitle(player, title, subtitle, color, 0.3f, 3.5f, 0.5f);
    }

    // Sound effects
    void playSound(PlayerRef player, String soundId, float volume, float pitch);

    // Reading methods for Kit saving
    java.util.List<ItemStack> getInventory(PlayerRef player);
    ItemStack[] getArmor(PlayerRef player); // [helmet, chestplate, leggings, boots]
}
