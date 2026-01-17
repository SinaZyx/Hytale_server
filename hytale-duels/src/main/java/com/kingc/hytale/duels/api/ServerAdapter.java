package com.kingc.hytale.duels.api;

import java.util.Optional;
import java.util.UUID;

public interface ServerAdapter {
    long nowEpochMs();
    Optional<PlayerRef> getPlayer(UUID id);
    Optional<PlayerRef> getPlayerByName(String name);
    void giveItems(PlayerRef player, ItemStack... items);
    void setArmor(PlayerRef player, ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots);
    void clearInventory(PlayerRef player);
    void applyEffect(PlayerRef player, String effectType, int amplifier, int durationTicks);
    void clearEffects(PlayerRef player);
}
