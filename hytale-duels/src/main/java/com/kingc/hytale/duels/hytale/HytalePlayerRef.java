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
         // Teleport logic currently disabled/stubbed
         // performTeleport(null, location); 
    }

    private void performTeleport(Player serverPlayer, Location location) {
        // Teleport logic disabled due to API mismatches (teleport/getWorldUuid)
    }

    @Override
    public boolean isOnline() {
        return Universe.get().getPlayer(playerRef.getUuid()) != null;
    }

    public PlayerRef hytale() {
        return playerRef;
    }
}
