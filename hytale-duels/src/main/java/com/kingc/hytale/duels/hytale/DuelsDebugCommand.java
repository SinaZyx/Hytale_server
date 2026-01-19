package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;

public class DuelsDebugCommand extends CommandBase {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final HytaleDuelsPlugin plugin;

    public DuelsDebugCommand(HytaleDuelsPlugin plugin) {
        super("duelsdebug", "Debug command to list player components");
        this.plugin = plugin;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("Only players can use this command."));
            return;
        }

        CommandSender sender = ctx.sender();
        if (sender == null) return;

        ctx.sendMessage(Message.raw("Inspecting your components..."));
        LOGGER.atInfo().log("Starting debug inspection for player UUID: " + sender.getUuid());

        PlayerRef playerRef = Universe.get().getPlayer(sender.getUuid());
        if (playerRef == null) {
             ctx.sendMessage(Message.raw("PlayerRef not found."));
             return;
        }
        
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            ctx.sendMessage(Message.raw("World not found."));
            return;
        }
        
        world.execute(() -> {
            EntityStore entityStore = world.getEntityStore();
            if (entityStore == null) {
                ctx.sendMessage(Message.raw("EntityStore is null."));
                return;
            }

            Ref<EntityStore> ref = entityStore.getRefFromUUID(playerRef.getUuid());
            Store<EntityStore> store = entityStore.getStore();

            if (ref == null || store == null) {
                ctx.sendMessage(Message.raw("Store or Ref is null."));
                return;
            }

            // Inspect Components
            // Since we can't iterate components easily without knowing their types (ECS limitation usually),
            // We will try to dump the 'toString' of the entity or finding a way to get all components.
            // If the API doesn't allow 'getAllComponents', we might have to use reflection on the Store/Ref.
            
            // For now, let's try to print the Entity/Player object itself, it might dump its components in toString()
            Object playerEntity = store.getComponent(ref, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
            if (playerEntity != null) {
                 LOGGER.atInfo().log("Player Entity Class: " + playerEntity.getClass().getName());
                 LOGGER.atInfo().log("Player Entity toString: " + playerEntity.toString());
                 
                 // Reflection: Find interesting methods
                 LOGGER.atInfo().log("Scanning methods...");
                 for (java.lang.reflect.Method m : playerEntity.getClass().getMethods()) {
                     String name = m.getName().toLowerCase();
                     if (name.contains("teleport") || name.contains("health") || name.contains("pos") || name.contains("loc")) {
                         LOGGER.atInfo().log("Found method: " + m.getName() + " returns " + m.getReturnType().getSimpleName());
                     }
                 }
            } else {
                 LOGGER.atInfo().log("Player Entity Component not found.");
            }
            
            ctx.sendMessage(Message.raw("Check server console for debug details."));
        });
    }
}
