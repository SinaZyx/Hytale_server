package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.kingc.hytale.duels.api.ItemStack;
import com.kingc.hytale.duels.api.Location;
import com.kingc.hytale.duels.api.ServerAdapter;

import java.util.Optional;
import java.util.UUID;

public final class HytaleServerAdapter implements ServerAdapter {

    @Override
    public long nowEpochMs() {
        return System.currentTimeMillis();
    }

    @Override
    public Optional<com.kingc.hytale.duels.api.PlayerRef> getPlayer(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        PlayerRef ref = Universe.get().getPlayer(id);
        if (ref == null) {
            return Optional.empty();
        }
        return Optional.of(new HytalePlayerRef(ref));
    }

    @Override
    public Optional<com.kingc.hytale.duels.api.PlayerRef> getPlayerByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        PlayerRef ref = Universe.get().getPlayerByUsername(name, NameMatching.EXACT_IGNORE_CASE);
        if (ref == null) {
            return Optional.empty();
        }
        return Optional.of(new HytalePlayerRef(ref));
    }

    @Override
    public void giveItems(com.kingc.hytale.duels.api.PlayerRef player, ItemStack... items) {
        // TODO: Implement using correct Hytale API
    }

    @Override
    public void setArmor(com.kingc.hytale.duels.api.PlayerRef player, ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
        // TODO: Implement using correct Hytale API
    }

    @Override
    public void clearInventory(com.kingc.hytale.duels.api.PlayerRef player) {
         // TODO: Implement using correct Hytale API
    }

    @Override
    public void applyEffect(com.kingc.hytale.duels.api.PlayerRef player, String effectType, int amplifier, int durationTicks) {
         // TODO: Implement using correct Hytale API
    }

    @Override
    public void clearEffects(com.kingc.hytale.duels.api.PlayerRef player) {
         // TODO: Implement using correct Hytale API
    }

    // === Methodes utilitaires ===

    public Optional<Location> getPlayerLocation(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        PlayerRef ref = Universe.get().getPlayer(id);
        if (ref == null) {
            return Optional.empty();
        }
        Transform transform = ref.getTransform();
        if (transform == null) {
            return Optional.empty();
        }
        Vector3d position = transform.getPosition();
        Vector3f rotation = transform.getRotation();
        if (position == null || rotation == null) {
            return Optional.empty();
        }
        World world = Universe.get().getWorld(ref.getWorldUuid());
        String worldName = world != null ? world.getName() : ref.getWorldUuid().toString();
        return Optional.of(new Location(
            worldName,
            position.getX(),
            position.getY(),
            position.getZ(),
            rotation.getYaw(),
            rotation.getPitch()
        ));
    }

    private void performTeleport(Player serverPlayer, Location location) {
         // Teleport logic disabled due to API mismatches
    }
}
