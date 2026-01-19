package com.kingc.hytale.factions.hytale;

import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.Transform;
import com.hypixel.hytale.protocol.packets.worldmap.MapChunk;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.protocol.packets.worldmap.MapImage;
import com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMap;
import com.hypixel.hytale.server.core.asset.type.gameplay.GameplayConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.kingc.hytale.factions.FactionsPlugin;
import com.kingc.hytale.factions.model.ClaimKey;
import com.kingc.hytale.factions.model.Faction;
import com.kingc.hytale.factions.service.FactionSettings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

public final class FactionWorldMapProvider implements WorldMapManager.MarkerProvider {
    private static final String CLAIM_MARKER_IMAGE = "Home.png";
    private static final int OVERLAY_ALPHA = 96;
    private static final int DEFAULT_OWN_COLOR = 0x00FF00;
    private static final int DEFAULT_ALLY_COLOR = 0x00FFFF;

    private final FactionsPlugin plugin;
    private final LongSupplier timeProvider;
    private final Map<UUID, Long> activeUntil;
    private final Map<UUID, CachedMarkers> markerCache = new ConcurrentHashMap<>();

    public FactionWorldMapProvider(FactionsPlugin plugin, LongSupplier timeProvider, Map<UUID, Long> activeUntil) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
        this.activeUntil = Objects.requireNonNull(activeUntil, "activeUntil");
    }

    @Override
    public void update(World world, GameplayConfig config, WorldMapTracker tracker, int viewRadius, int centerChunkX, int centerChunkZ) {
        if (world == null || tracker == null) {
            return;
        }
        Player viewer = tracker.getPlayer();
        if (viewer == null) {
            return;
        }
        UUID viewerId = viewer.getUuid();
        long now = timeProvider.getAsLong();
        CachedMarkers cached = markerCache.get(viewerId);
        Long until = activeUntil.get(viewerId);
        if (until == null || now >= until) {
            activeUntil.remove(viewerId);
            markerCache.remove(viewerId);
            tracker.setPlayerMapFilter(null);
            if (cached != null) {
                restoreClaimOverlays(findWorldByName(cached.worldName()), viewerId, cached.tintedClaims());
            }
            return;
        }

        if (cached != null && !world.getName().equals(cached.worldName())) {
            restoreClaimOverlays(findWorldByName(cached.worldName()), viewerId, cached.tintedClaims());
            cached = null;
        }

        FactionSettings settings = plugin.settings();
        long intervalMs = Math.max(1000L, settings.worldMapUpdateIntervalSeconds * 1000L);

        if (cached == null || cached.isExpired(now, intervalMs) || !world.getName().equals(cached.worldName())) {
            cached = buildMarkers(world, viewerId, viewRadius, centerChunkX, centerChunkZ, now, cached);
            markerCache.put(viewerId, cached);
        }

        tracker.setPlayerMapFilter(cached.playerFilter());
        for (MapMarker marker : cached.markers()) {
            tracker.trySendMarker(viewRadius, centerChunkX, centerChunkZ, marker);
        }
    }

    private CachedMarkers buildMarkers(World world, UUID viewerId, int viewRadius, int centerChunkX,
                                       int centerChunkZ, long now, CachedMarkers previous) {
        List<MapMarker> markers = new ArrayList<>();
        Faction viewerFaction = plugin.service().findFactionByMember(viewerId).orElse(null);
        if (viewerFaction == null) {
            if (previous != null) {
                restoreClaimOverlays(world, viewerId, previous.tintedClaims());
            }
            return new CachedMarkers(world.getName(), now, markers, playerRef -> true, new HashSet<>());
        }

        Set<ClaimKey> tintedClaims = previous == null ? new HashSet<>() : new HashSet<>(previous.tintedClaims());
        Set<UUID> visibleFactions = new HashSet<>();
        visibleFactions.add(viewerFaction.id());
        visibleFactions.addAll(viewerFaction.allies());

        // Claim markers (same faction + allies)
        Map<ClaimKey, UUID> claims = plugin.service().getClaimsInRadius(world.getName(), centerChunkX, centerChunkZ, viewRadius);
        List<ClaimOverlay> overlays = new ArrayList<>();
        int ownColor = parseRgb(plugin.settings().colorOwn, DEFAULT_OWN_COLOR);
        int allyColor = parseRgb(plugin.settings().colorAlly, DEFAULT_ALLY_COLOR);
        for (Map.Entry<ClaimKey, UUID> entry : claims.entrySet()) {
            UUID ownerId = entry.getValue();
            if (!visibleFactions.contains(ownerId)) {
                continue;
            }
            Faction ownerFaction = plugin.service().getFactionById(ownerId).orElse(null);
            if (ownerFaction == null) {
                continue;
            }
            ClaimKey claim = entry.getKey();
            markers.add(buildClaimMarker(claim, ownerFaction.name()));
            int overlayColor = ownerId.equals(viewerFaction.id()) ? ownColor : allyColor;
            overlays.add(new ClaimOverlay(claim, overlayColor));
            tintedClaims.add(claim);
        }
        sendClaimOverlays(world, viewerId, overlays);

        Predicate<com.hypixel.hytale.server.core.universe.PlayerRef> filter = playerRef -> {
            if (playerRef == null) {
                return true;
            }
            if (viewerId.equals(playerRef.getUuid())) {
                return false;
            }
            Faction targetFaction = plugin.service().findFactionByMember(playerRef.getUuid()).orElse(null);
            return targetFaction == null || !visibleFactions.contains(targetFaction.id());
        };

        return new CachedMarkers(world.getName(), now, markers, filter, tintedClaims);
    }

    private MapMarker buildClaimMarker(ClaimKey claim, String factionName) {
        FactionSettings settings = plugin.settings();
        double size = settings.chunkSize;
        double centerX = (claim.x() + 0.5d) * size;
        double centerZ = (claim.z() + 0.5d) * size;
        String id = "faction:claim:" + claim.world() + ":" + claim.x() + ":" + claim.z();
        String name = "Claim " + factionName;
        Transform packetTransform = new Transform(
                new Position(centerX, 0d, centerZ),
                new Direction(0f, 0f, 0f)
        );
        return new MapMarker(id, name, CLAIM_MARKER_IMAGE, packetTransform, null);
    }

    private void sendClaimOverlays(World world, UUID viewerId, List<ClaimOverlay> overlays) {
        if (world == null || overlays.isEmpty()) {
            return;
        }
        WorldMapManager manager = world.getWorldMapManager();
        if (manager == null) {
            return;
        }
        List<MapChunk> chunks = new ArrayList<>();
        for (ClaimOverlay overlay : overlays) {
            ClaimKey claim = overlay.claim();
            MapImage base = manager.getImageIfInMemory(claim.x(), claim.z());
            if (base == null || base.data == null) {
                continue;
            }
            MapImage tinted = base.clone();
            applyOverlay(tinted, overlay.color(), OVERLAY_ALPHA);
            chunks.add(new MapChunk(claim.x(), claim.z(), tinted));
        }
        sendChunks(viewerId, chunks);
    }

    private void restoreClaimOverlays(World world, UUID viewerId, Set<ClaimKey> claims) {
        if (world == null || claims == null || claims.isEmpty()) {
            return;
        }
        WorldMapManager manager = world.getWorldMapManager();
        if (manager == null) {
            return;
        }
        List<MapChunk> chunks = new ArrayList<>();
        for (ClaimKey claim : claims) {
            if (!world.getName().equals(claim.world())) {
                continue;
            }
            MapImage base = manager.getImageIfInMemory(claim.x(), claim.z());
            if (base == null) {
                continue;
            }
            chunks.add(new MapChunk(claim.x(), claim.z(), base));
        }
        sendChunks(viewerId, chunks);
    }

    private void sendChunks(UUID viewerId, List<MapChunk> chunks) {
        if (viewerId == null || chunks.isEmpty()) {
            return;
        }
        PlayerRef playerRef = Universe.get().getPlayer(viewerId);
        if (playerRef == null) {
            return;
        }
        PacketHandler handler = playerRef.getPacketHandler();
        if (handler == null) {
            return;
        }
        handler.writeNoCache(new UpdateWorldMap(chunks.toArray(new MapChunk[0]), null, null));
    }

    private static void applyOverlay(MapImage image, int rgb, int alpha) {
        if (image == null || image.data == null) {
            return;
        }
        int[] data = image.data;
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        for (int i = 0; i < data.length; i++) {
            data[i] = blendPixel(data[i], red, green, blue, alpha);
        }
    }

    private static int blendPixel(int base, int overlayR, int overlayG, int overlayB, int overlayA) {
        int baseR = (base >> 24) & 0xFF;
        int baseG = (base >> 16) & 0xFF;
        int baseB = (base >> 8) & 0xFF;
        int baseA = base & 0xFF;
        int invA = 255 - overlayA;
        int outR = (overlayR * overlayA + baseR * invA) / 255;
        int outG = (overlayG * overlayA + baseG * invA) / 255;
        int outB = (overlayB * overlayA + baseB * invA) / 255;
        return (outR << 24) | (outG << 16) | (outB << 8) | baseA;
    }

    private static int parseRgb(String hex, int fallback) {
        if (hex == null) {
            return fallback;
        }
        String value = hex.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseInt(value, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static World findWorldByName(String name) {
        if (name == null) {
            return null;
        }
        for (World world : Universe.get().getWorlds().values()) {
            if (name.equals(world.getName())) {
                return world;
            }
        }
        return null;
    }

    private record ClaimOverlay(ClaimKey claim, int color) {}

    private record CachedMarkers(String worldName, long updatedAt, List<MapMarker> markers,
                                 Predicate<com.hypixel.hytale.server.core.universe.PlayerRef> playerFilter,
                                 Set<ClaimKey> tintedClaims) {
        boolean isExpired(long now, long intervalMs) {
            return now - updatedAt >= intervalMs;
        }
    }
}
