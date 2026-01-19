package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.server.core.event.events.ShutdownEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.kingc.hytale.duels.DuelsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;

import javax.annotation.Nonnull;
import java.io.IOException;

public final class HytaleDuelsPlugin extends JavaPlugin {
    private static final com.hypixel.hytale.logger.HytaleLogger LOGGER = com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private DuelsPlugin core;
    private HytaleServerAdapter serverAdapter;
    private HytaleDeathScanner deathScanner;

    public HytaleDuelsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        try {
            this.serverAdapter = new HytaleServerAdapter(this);
            this.core = new DuelsPlugin(getDataDirectory(), serverAdapter);
            
            this.deathScanner = new HytaleDeathScanner(this);
            this.deathScanner.start();

            registerCommands();
            registerEvents();

            LOGGER.atInfo().log("HytaleDuels enabled!");
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Failed to initialize HytaleDuels", e.getMessage());
        }
    }

    private void registerCommands() {
        var registry = getCommandRegistry();
        registry.registerCommand(new DuelCommand(this));
        registry.registerCommand(new QueueCommand(this));
        registry.registerCommand(new KitCommand(this));
        registry.registerCommand(new StatsCommand(this));
        registry.registerCommand(new TopCommand(this));
        registry.registerCommand(new RankingCommand(this));
        registry.registerCommand(new DuelsAdminCommand(this));
        registry.registerCommand(new DuelsDebugCommand(this));
        registry.registerCommand(new PipeCommand(this));
        registry.registerCommand(new SetLobbyCommand(this));
    }

    private void registerEvents() {
        getEventRegistry().register(ShutdownEvent.class, event -> {
            if (deathScanner != null) deathScanner.stop();
            flush();
        });
    }

    public DuelsPlugin core() {
        return core;
    }

    public void openRankingMenu(com.hypixel.hytale.server.core.universe.PlayerRef playerRef) {
        if (playerRef == null) return;
        com.hypixel.hytale.server.core.universe.world.World world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(playerRef.getWorldUuid());
        if (world != null) {
            world.execute(() -> {
                 var entityStore = world.getEntityStore();
                 if (entityStore != null) {
                     var store = entityStore.getStore();
                     var ref = entityStore.getRefFromUUID(playerRef.getUuid());
                     if (store != null && ref != null) {
                         var player = store.getComponent(ref, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
                         if (player != null) {
                             player.getPageManager().openCustomPage(ref, store, new RankingMenuPage(this, playerRef));
                         }
                     }
                 }
            });
        }
    }

    public void openAdminMenu(com.hypixel.hytale.server.core.universe.PlayerRef playerRef) {
        if (playerRef == null) return;
        com.hypixel.hytale.server.core.universe.world.World world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(playerRef.getWorldUuid());
        if (world != null) {
            world.execute(() -> {
                 var entityStore = world.getEntityStore();
                 if (entityStore != null) {
                     var store = entityStore.getStore();
                     var ref = entityStore.getRefFromUUID(playerRef.getUuid());
                     if (store != null && ref != null) {
                         var player = store.getComponent(ref, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
                         if (player != null) {
                             player.getPageManager().openCustomPage(ref, store, new DuelsAdminPage(this, playerRef));
                         }
                     }
                 }
            });
        }
    }

    public void openHelpMenu(com.hypixel.hytale.server.core.universe.PlayerRef playerRef) {
        if (playerRef == null) return;
        com.hypixel.hytale.server.core.universe.world.World world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(playerRef.getWorldUuid());
        if (world != null) {
            world.execute(() -> {
                 var entityStore = world.getEntityStore();
                 if (entityStore != null) {
                     var store = entityStore.getStore();
                     var ref = entityStore.getRefFromUUID(playerRef.getUuid());
                     if (store != null && ref != null) {
                         var player = store.getComponent(ref, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
                         if (player != null) {
                             player.getPageManager().openCustomPage(ref, store, new HelpMenuPage(this, playerRef));
                         }
                     }
                 }
            });
        }
    }

    public void flush() {
        if (core != null) {
            try {
                core.save();
            } catch (IOException e) {
                LOGGER.atSevere().withCause(e).log("Failed to save HytaleDuels data", e.getMessage());
            }
        }
    }
}
