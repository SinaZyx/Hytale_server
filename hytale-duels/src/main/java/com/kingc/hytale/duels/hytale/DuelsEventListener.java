package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kingc.hytale.duels.DuelsPlugin;

public class DuelsEventListener {
    private final DuelsPlugin core;

    public DuelsEventListener(DuelsPlugin core) {
        this.core = core;
    }

    public void onPlayerDeath(KillFeedEvent event) {
        // KillFeedEvent is complex, often using nested classes for specific messages
        // But let's assume we can inspect it or it's a wrapper.
        // The guide mentions KillFeedEvent.KillerMessage and DecedentMessage.
        // We will listen to the base event if registered, or specific ones.
        // Assuming we registered KillFeedEvent.class:

        Ref<EntityStore> victimRef = null;

        if (event instanceof KillFeedEvent.KillerMessage) {
            victimRef = ((KillFeedEvent.KillerMessage) event).getTargetRef();
        } else if (event instanceof KillFeedEvent.DecedentMessage) {
            // Need to find how to get victim from DecedentMessage, often it is the context
            // But usually KillerMessage is enough for PvP
        }

        if (victimRef != null && victimRef.isValid()) {
             Store<EntityStore> store = victimRef.getStore();
             Player player = store.getComponent(victimRef, Player.getComponentType());
             // We need PlayerRef usually for IDs, or get UUID from UUIDComponent
             // Player component usually has access or we can get PlayerRef component if it exists
             // Or use HytaleServerAdapter logic to find UUID.

             // Actually, the easiest way to get UUID from Ref<EntityStore>:
             // It's not direct unless we have the component.
             // HytalePlayerRef wrapper uses:
             // playerEntity.getStore().getComponent(playerEntity, PlayerRef.getComponentType())

             PlayerRef playerRef = store.getComponent(victimRef, PlayerRef.getComponentType());
             if (playerRef != null) {
                 core.onPlayerDeath(playerRef.getUuid());
             }
        }
    }
}
