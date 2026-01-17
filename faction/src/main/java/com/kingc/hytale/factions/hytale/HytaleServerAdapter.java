package com.kingc.hytale.factions.hytale;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.kingc.hytale.factions.api.Location;
import com.kingc.hytale.factions.api.ServerAdapter;

import java.util.Optional;
import java.util.UUID;

public final class HytaleServerAdapter implements ServerAdapter {
    @Override
    public Optional<com.kingc.hytale.factions.api.PlayerRef> findOnlinePlayerByName(String name) {
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
    public Optional<com.kingc.hytale.factions.api.PlayerRef> findOnlinePlayer(UUID id) {
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
    public Optional<UUID> resolvePlayerId(String name) {
        return findOnlinePlayerByName(name).map(com.kingc.hytale.factions.api.PlayerRef::id);
    }

    @Override
    public Optional<String> resolvePlayerName(UUID id) {
        return findOnlinePlayer(id).map(com.kingc.hytale.factions.api.PlayerRef::name);
    }

    @Override
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

    @Override
    public void sendMessage(UUID id, String message) {
        if (id == null) {
            return;
        }
        PlayerRef ref = Universe.get().getPlayer(id);
        if (ref == null) {
            return;
        }
        ref.sendMessage(Message.raw(message));
    }

    @Override
    public long nowEpochMs() {
        return System.currentTimeMillis();
    }
}
