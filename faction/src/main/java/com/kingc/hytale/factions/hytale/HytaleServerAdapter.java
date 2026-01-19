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
                rotation.getPitch()));
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
        ref.sendMessage(FactionMessages.parseAndFormat(message));
    }

    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors
            .newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Factions-Scheduler");
                t.setDaemon(true);
                return t;
            });

    @Override
    public void schedule(Runnable runnable, long delay, java.util.concurrent.TimeUnit unit) {
        scheduler.schedule(runnable, delay, unit);
    }

    @Override
    public void teleportPlayer(UUID playerId, Location location) {
        if (playerId == null || location == null) {
            return;
        }
        PlayerRef ref = Universe.get().getPlayer(playerId);
        if (ref == null) {
            return;
        }

        World targetWorld = null;
        for (World w : Universe.get().getWorlds().values()) {
            if (w.getName().equals(location.world())) {
                targetWorld = w;
                break;
            }
        }

        if (targetWorld == null) {
            // Fallback: try to resolve by UUID if the string is a UUID?
            // Assuming location.world() is name.
            return;
        }

        // Accessing ECS components requires casting/imports
        com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> entityRef = ref
                .getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store = entityRef
                .getStore();
        if (store == null) {
            return;
        }

        World currentWorld = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store
                .getExternalData()).getWorld();

        final World finalTargetWorld = targetWorld;
        currentWorld.execute(() -> {
            com.hypixel.hytale.server.core.modules.entity.teleport.Teleport teleport = new com.hypixel.hytale.server.core.modules.entity.teleport.Teleport(
                    finalTargetWorld,
                    new Vector3d(location.x(), location.y(), location.z()),
                    new Vector3f(0f, location.yaw(), location.pitch()));
            store.addComponent(entityRef,
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), teleport);
        });
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    @Override
    public void teleportDelayed(UUID playerId, Location target, long delaySeconds, Runnable onSuccess,
            Runnable onCancel) {
        final Optional<Location> startOpt = getPlayerLocation(playerId);
        if (startOpt.isEmpty()) {
            return;
        }
        final Location startLoc = startOpt.get();

        schedule(() -> {
            Optional<Location> currentOpt = getPlayerLocation(playerId);
            if (currentOpt.isEmpty()) {
                return;
            }
            Location currentLoc = currentOpt.get();

            if (!currentLoc.world().equals(startLoc.world()) || distanceSquared(startLoc, currentLoc) > 1.0) {
                if (onCancel != null)
                    onCancel.run();
                return;
            }

            teleportPlayer(playerId, target);
            if (onSuccess != null)
                onSuccess.run();
        }, delaySeconds, java.util.concurrent.TimeUnit.SECONDS);
    }

    private double distanceSquared(Location l1, Location l2) {
        double dx = l1.x() - l2.x();
        double dy = l1.y() - l2.y();
        double dz = l1.z() - l2.z();
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public long nowEpochMs() {
        return System.currentTimeMillis();
    }
}
