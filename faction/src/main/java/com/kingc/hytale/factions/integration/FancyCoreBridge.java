package com.kingc.hytale.factions.integration;

import com.fancyinnovations.fancycore.api.FancyCore;
import com.fancyinnovations.fancycore.api.economy.Currency;
import com.fancyinnovations.fancycore.api.economy.CurrencyService;
import com.fancyinnovations.fancycore.api.permissions.PermissionService;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.api.teleport.Warp;
import com.fancyinnovations.fancycore.api.teleport.WarpService;
import com.kingc.hytale.factions.api.Location;
import com.kingc.hytale.factions.model.Faction;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class FancyCoreBridge {
    private static final String FACTION_WARP_PREFIX = "faction_";
    private static final boolean CLASS_AVAILABLE = isFancyCorePresent();

    private FancyCoreBridge() {
    }

    public static boolean isAvailable() {
        if (!CLASS_AVAILABLE) {
            return false;
        }
        return FancyCore.get() != null;
    }

    public static Optional<Boolean> checkPermission(UUID playerId, String permission) {
        if (playerId == null || permission == null) {
            return Optional.empty();
        }
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            return Optional.of(PermissionService.get().hasPermission(playerId, permission));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    public static Optional<FancyPlayer> getFancyPlayer(UUID playerId) {
        if (playerId == null || !isAvailable()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(FancyPlayerService.get().getByUUID(playerId));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    public static Optional<Currency> getPrimaryCurrency() {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(CurrencyService.get().getPrimaryCurrency());
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    public static void upsertFactionHomeWarp(Faction faction, Location home) {
        if (faction == null || home == null || !isAvailable()) {
            return;
        }
        try {
            Warp warp = new Warp(warpNameForFaction(faction.name()), toFancyLocation(home));
            WarpService.get().setWarp(warp);
        } catch (Throwable ignored) {
            // FancyCore may be missing or not fully initialized.
        }
    }

    public static void removeFactionHomeWarp(String factionName) {
        if (factionName == null || !isAvailable()) {
            return;
        }
        try {
            WarpService.get().deleteWarp(warpNameForFaction(factionName));
        } catch (Throwable ignored) {
            // FancyCore may be missing or not fully initialized.
        }
    }

    private static com.fancyinnovations.fancycore.api.teleport.Location toFancyLocation(Location location) {
        return new com.fancyinnovations.fancycore.api.teleport.Location(
                location.world(),
                location.x(),
                location.y(),
                location.z(),
                location.yaw(),
                location.pitch()
        );
    }

    private static String warpNameForFaction(String factionName) {
        return FACTION_WARP_PREFIX + sanitize(factionName);
    }

    private static String sanitize(String name) {
        if (name == null) {
            return "unknown";
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isFancyCorePresent() {
        try {
            Class.forName("com.fancyinnovations.fancycore.main.FancyCorePlugin");
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
