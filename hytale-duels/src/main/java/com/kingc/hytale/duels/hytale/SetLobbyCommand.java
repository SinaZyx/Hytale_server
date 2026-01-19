package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kingc.hytale.duels.api.Location;

import javax.annotation.Nonnull;

public class SetLobbyCommand extends AbstractPlayerCommand {

    private final HytaleDuelsPlugin plugin;

    public SetLobbyCommand(HytaleDuelsPlugin plugin) {
        super("setlobby", "Sets the duels lobby location");
        this.plugin = plugin;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
        
        if (transform == null) {
            ctx.sendMessage(Message.raw("&cCould not determine your location."));
            return;
        }
        
        Vector3d pos = transform.getPosition();
        Vector3f rot = headRotation != null ? headRotation.getRotation() : new Vector3f(0, 0, 0);
        
        Location loc = new Location(
            world.getName(),
            pos.getX(), pos.getY(), pos.getZ(),
            rot.getYaw(), rot.getPitch()
        );
        
        plugin.core().setLobbySpawn(loc);
        ctx.sendMessage(Message.raw("&aLobby location set!"));
    }
}
