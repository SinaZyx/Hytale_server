package com.fancyinnovations.fancycore.commands.teleport;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportGuard {
    private static final long TELEPORT_COOLDOWN_MS = 3000L;
    private static final long COMBAT_BLOCK_MS = 10000L;

    private static final Map<UUID, Long> LAST_COMBAT_AT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_TELEPORT_AT = new ConcurrentHashMap<>();

    private TeleportGuard() {
    }

    public static void recordCombat(UUID playerId) {
        if (playerId == null) {
            return;
        }
        LAST_COMBAT_AT.put(playerId, System.currentTimeMillis());
    }

    public static String checkSender(UUID playerId) {
        long now = System.currentTimeMillis();
        String combatBlock = checkCombat(playerId, now);
        if (combatBlock != null) {
            return combatBlock;
        }
        return checkCooldown(playerId, now);
    }

    public static String checkTarget(UUID playerId) {
        return checkCombat(playerId, System.currentTimeMillis());
    }

    public static void markTeleport(UUID playerId) {
        if (playerId == null) {
            return;
        }
        LAST_TELEPORT_AT.put(playerId, System.currentTimeMillis());
    }

    private static String checkCombat(UUID playerId, long now) {
        if (playerId == null) {
            return null;
        }
        Long lastCombat = resolveLastCombat(playerId);
        if (lastCombat == null) {
            return null;
        }
        long remainingMs = COMBAT_BLOCK_MS - (now - lastCombat);
        if (remainingMs <= 0) {
            return null;
        }
        return "Teleport impossible en combat. Temps restant: " + secondsLeft(remainingMs) + "s.";
    }

    private static String checkCooldown(UUID playerId, long now) {
        if (playerId == null) {
            return null;
        }
        Long lastTeleport = LAST_TELEPORT_AT.get(playerId);
        if (lastTeleport == null) {
            return null;
        }
        long remainingMs = TELEPORT_COOLDOWN_MS - (now - lastTeleport);
        if (remainingMs <= 0) {
            return null;
        }
        return "Cooldown teleport. Temps restant: " + secondsLeft(remainingMs) + "s.";
    }

    private static int secondsLeft(long remainingMs) {
        return (int) Math.ceil(remainingMs / 1000.0);
    }

    private static Long resolveLastCombat(UUID playerId) {
        Long lastCombat = LAST_COMBAT_AT.get(playerId);
        Long ecsCombat = resolveCombatFromComponent(playerId);
        if (ecsCombat == null) {
            return lastCombat;
        }
        if (lastCombat == null) {
            return ecsCombat;
        }
        return Math.max(lastCombat, ecsCombat);
    }

    private static Long resolveCombatFromComponent(UUID playerId) {
        PlayerRef playerRef = Universe.get().getPlayer(playerId);
        if (playerRef == null || !playerRef.isValid()) {
            return null;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }
        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return null;
        }
        DamageDataComponent damageData = store.getComponent(ref, DamageDataComponent.getComponentType());
        if (damageData == null) {
            return null;
        }
        Instant lastCombat = damageData.getLastCombatAction();
        if (lastCombat != null) {
            try {
                return lastCombat.toEpochMilli();
            } catch (ArithmeticException e) {
                // Instant value is too large to convert to milliseconds, ignore it
                return null;
            }
        }
        Instant lastDamage = damageData.getLastDamageTime();
        if (lastDamage != null) {
            try {
                return lastDamage.toEpochMilli();
            } catch (ArithmeticException e) {
                // Instant value is too large to convert to milliseconds, ignore it
                return null;
            }
        }
        return null;
    }
}
