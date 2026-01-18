package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.kingc.hytale.duels.api.Location;

import java.util.UUID;

public record HytalePlayerRef(PlayerRef playerRef) implements com.kingc.hytale.duels.api.PlayerRef {

    @Override
    public UUID id() {
        return playerRef.getUuid();
    }

    @Override
    public String name() {
        String name = playerRef.getUsername();
        return name != null ? name : "Unknown";
    }

    @Override
    public void sendMessage(String message) {
        playerRef.sendMessage(Message.raw(message));
    }

    @Override
    public void teleport(Location location) {
        if (location == null) return;

        World world = Universe.get().getWorld(location.world());
        if (world == null) {
            // Fallback to current world if target not found (or handle error)
            world = Universe.get().getWorld(playerRef.getWorldUuid());
        }

        if (world != null) {
            World targetWorld = world;
            targetWorld.execute(() -> {
                var entityStore = targetWorld.getEntityStore();
                if (entityStore != null) {
                    var store = entityStore.getStore();
                    var ref = entityStore.getRefFromUUID(playerRef.getUuid());
                    if (store != null && ref != null) {
                        try {
                            store.addComponent(ref, com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(),
                                new com.hypixel.hytale.server.core.modules.entity.teleport.Teleport(
                                    targetWorld,
                                    new Vector3d(location.x(), location.y(), location.z()),
                                    new Vector3f((float)location.yaw(), (float)location.pitch(), 0f)
                                )
                            );
                        } catch (Exception e) {
                           // Log/Handle error
                           System.err.println("Teleport failed: " + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    @Override
    public boolean isOnline() {
        return Universe.get().getPlayer(playerRef.getUuid()) != null;
    }

    public PlayerRef hytale() {
        return playerRef;
    }
}
