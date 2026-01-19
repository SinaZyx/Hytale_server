package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
// import com.hypixel.hytale.server.core.entity.component.Component; // Assumption

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HytaleDeathScanner {
    private static final Logger LOGGER = Logger.getLogger(HytaleDeathScanner.class.getName());
    
    private final HytaleDuelsPlugin plugin;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Duels-DeathScanner");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> task;

    public HytaleDeathScanner(HytaleDuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) return;
        task = scheduler.scheduleAtFixedRate(this::scan, 1, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
        scheduler.shutdownNow();
    }

    private void scan() {
        if (plugin.core() == null) return;

        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            UUID playerId = playerRef.getUuid();
            if (playerId == null) continue;

            // Only check players currently in a match
            if (!plugin.core().matchService().isInMatch(playerId)) {
                continue;
            }

            try {
                if (isDead(playerRef)) {
                    plugin.core().onPlayerDeath(playerId);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error checking player health for " + playerRef, e);
            }
        }
    }

    // Cached Health Component Class
    private Class<?> healthComponentClass = null;
    private boolean lookupFailed = false;

    private boolean isDead(PlayerRef playerRef) {
        // Only check players in a match
        if (!plugin.core().matchService().isInMatch(playerRef.getUuid())) {
             return false;
        }
        // Attempt 1: Check if Player class has getHealth() or similar
        // Since we don't know the exact API, checking logic via EntityStore if available
        
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) return false;

        // Execute on world thread for safety if needed, but scanner is async. 
        // Read-only checks might be fine or require scheduling. 
        // For simplicity, we assume we can read components.
        
        EntityStore entityStore = world.getEntityStore();
        if (entityStore == null) return false;

        Ref<EntityStore> ref = entityStore.getRefFromUUID(playerRef.getUuid());
        Store<EntityStore> store = entityStore.getStore();
        
        if (ref == null || store == null) return false;

        try {
            // Auto-Discovery: Try to find HealthComponent if not already found
            if (healthComponentClass == null && !lookupFailed) {
                 String[] potentialClasses = {
                     "com.hypixel.hytale.server.core.entity.component.health.HealthComponent",
                     "com.hypixel.hytale.component.HealthComponent", 
                     "com.hypixel.hytale.server.component.HealthComponent"
                 };
                 
                 for (String className : potentialClasses) {
                     try {
                         healthComponentClass = Class.forName(className);
                         // LOGGER.atInfo().log("Auto-discovered HealthComponent: " + className); 
                         break;
                     } catch (ClassNotFoundException ignored) {}
                 }
                 
                 if (healthComponentClass == null) {
                     lookupFailed = true;
                     // LOGGER.atWarning().log("Could not auto-discover HealthComponent. Scanner will not work."); 
                 }
            }

            if (healthComponentClass != null) {
                 // TODO: Implement actual health check when API is known
                 // For now, disabled to allow compilation
            }
            
            return false;
        } catch (Exception e) {
            return false; // Fail safe
        }
    }
}
