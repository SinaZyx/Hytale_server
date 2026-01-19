package com.fancyinnovations.fancycore.listeners;

import com.fancyinnovations.fancycore.commands.teleport.TeleportGuard;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

public final class CombatTagListener {

    private CombatTagListener() {
    }

    public static void onKillFeed(KillFeedEvent.KillerMessage event) {
        if (event == null) {
            return;
        }
        Damage damage = event.getDamage();
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource)) {
            return;
        }

        Ref<EntityStore> killerRef = ((Damage.EntitySource) source).getRef();
        Ref<EntityStore> victimRef = event.getTargetRef();
        UUID killerId = resolvePlayerUuid(killerRef);
        UUID victimId = resolvePlayerUuid(victimRef);
        if (killerId != null) {
            TeleportGuard.recordCombat(killerId);
        }
        if (victimId != null) {
            TeleportGuard.recordCombat(victimId);
        }
    }

    private static UUID resolvePlayerUuid(Ref<EntityStore> ref) {
        if (ref == null || !ref.isValid()) {
            return null;
        }
        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return null;
        }
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null || !playerRef.isValid()) {
            return null;
        }
        return playerRef.getUuid();
    }
}
