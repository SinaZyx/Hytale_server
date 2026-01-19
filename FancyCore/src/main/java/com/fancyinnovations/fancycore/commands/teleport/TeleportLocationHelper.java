package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.teleport.Location;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashMap;
import java.util.Map;

public class TeleportLocationHelper {

    /**
     * Saves the current location of a player as their previous location (for /back
     * command).
     * This should be called before teleporting the player.
     *
     * @param fp    The FancyPlayer to save the location for
     * @param ref   The entity reference
     * @param store The entity store
     * @param world The world the player is in
     */
    public static void savePreviousLocation(FancyPlayer fp, Ref<EntityStore> ref, Store<EntityStore> store,
            World world) {
        TransformComponent transformComponent = (TransformComponent) store.getComponent(ref,
                TransformComponent.getComponentType());
        if (transformComponent == null) {
            return;
        }

        HeadRotation headRotationComponent = (HeadRotation) store.getComponent(ref, HeadRotation.getComponentType());
        if (headRotationComponent == null) {
            return;
        }

        Map<String, Object> backLocation = new HashMap<>();
        backLocation.put("world", world.getName());
        backLocation.put("x", transformComponent.getPosition().getX());
        backLocation.put("y", transformComponent.getPosition().getY());
        backLocation.put("z", transformComponent.getPosition().getZ());
        backLocation.put("yaw", headRotationComponent.getRotation().getYaw());
        backLocation.put("pitch", headRotationComponent.getRotation().getPitch());
        fp.getData().setCustomData("teleport_back_location", backLocation);
    }

    public static void teleportDelayed(FancyPlayer fp, Location target, long delaySeconds, Runnable onSuccess,
            Runnable onCancel) {
        com.hypixel.hytale.server.core.universe.PlayerRef playerRef = fp.getPlayer();
        if (playerRef == null)
            return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid())
            return;

        Store<EntityStore> store = ref.getStore();
        if (store == null)
            return;

        TransformComponent startTransform = (TransformComponent) store.getComponent(ref,
                TransformComponent.getComponentType());
        if (startTransform == null)
            return;

        final com.hypixel.hytale.math.vector.Vector3d startPos = startTransform.getPosition().clone();
        final String startWorld = playerRef.getWorldUuid().toString();

        com.fancyinnovations.fancycore.main.FancyCorePlugin.get().getThreadPool().schedule(() -> {
            com.hypixel.hytale.server.core.universe.PlayerRef currentRef = fp.getPlayer();
            if (currentRef == null)
                return;

            Ref<EntityStore> cRef = currentRef.getReference();
            if (cRef == null || !cRef.isValid())
                return;

            Store<EntityStore> cStore = cRef.getStore();
            if (cStore == null)
                return;

            TransformComponent currentTransform = (TransformComponent) cStore.getComponent(cRef,
                    TransformComponent.getComponentType());
            if (currentTransform == null)
                return;

            com.hypixel.hytale.math.vector.Vector3d currentPos = currentTransform.getPosition();
            String currentWorld = currentRef.getWorldUuid().toString();

            double dx = startPos.getX() - currentPos.getX();
            double dy = startPos.getY() - currentPos.getY();
            double dz = startPos.getZ() - currentPos.getZ();
            double distSq = dx * dx + dy * dy + dz * dz;

            if (!startWorld.equals(currentWorld) || distSq > 1.0) {
                if (onCancel != null)
                    onCancel.run();
                return;
            }

            // Teleport directly using Teleport component
            World targetWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(target.worldName());
            if (targetWorld == null)
                return;

            com.hypixel.hytale.server.core.modules.entity.teleport.Teleport teleport = new com.hypixel.hytale.server.core.modules.entity.teleport.Teleport(
                    targetWorld, target.positionVec(), target.rotationVec());
            cStore.addComponent(cRef,
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), teleport);

            if (onSuccess != null)
                onSuccess.run();

        }, delaySeconds, java.util.concurrent.TimeUnit.SECONDS);
    }
}
