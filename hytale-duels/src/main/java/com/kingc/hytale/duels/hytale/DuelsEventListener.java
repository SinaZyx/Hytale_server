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

    public void onPlayerDeath(KillFeedEvent.KillerMessage event) {
        // Corrected signature to directly accept KillerMessage since we register for it specifically
        Ref<EntityStore> victimRef = event.getTargetRef();

        if (victimRef != null && victimRef.isValid()) {
             Store<EntityStore> store = victimRef.getStore();
             // Assuming HytalePlayerRef logic for consistency or just direct UUID retrieval
             PlayerRef playerRef = store.getComponent(victimRef, PlayerRef.getComponentType());
             if (playerRef != null) {
                 core.onPlayerDeath(playerRef.getUuid());
             }
        }
    }
}
