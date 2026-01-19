package com.kingc.hytale.factions.hytale;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ShutdownEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kingc.hytale.factions.FactionsPlugin;
import com.kingc.hytale.factions.api.ClaimChangeType;
import com.kingc.hytale.factions.api.Location;
import com.kingc.hytale.factions.model.ClaimKey;
import com.kingc.hytale.factions.model.Faction;
import com.kingc.hytale.factions.model.MemberRole;
import com.kingc.hytale.factions.model.NotificationType;
import com.kingc.hytale.factions.model.War;
import com.kingc.hytale.factions.service.CombatService;
import com.kingc.hytale.factions.service.FactionSettings;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.kingc.hytale.factions.model.ChatMode;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.fancyinnovations.fancycore.api.economy.Currency;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.kingc.hytale.factions.api.FactionsApi;
import com.kingc.hytale.factions.api.event.FactionDisbandedEvent;
import com.kingc.hytale.factions.api.event.FactionHomeSetEvent;
import com.kingc.hytale.factions.api.event.FactionRenamedEvent;
import com.kingc.hytale.factions.integration.FancyCoreBridge;
import com.kingc.hytale.factions.service.Result;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;


import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class HytaleFactionsPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String WILDERNESS_KEY = "WILDERNESS";
    private static final String DEFAULT_PARTICLE_ASSET = "hytale:smoke";

    private final HytaleServerAdapter serverAdapter = new HytaleServerAdapter();
    private final ScheduledExecutorService claimScanner = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "HytaleFactions-ClaimScanner");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<UUID, String> lastClaimByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastClaimNotifyAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> borderViewUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastBorderParticleAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastInvasionSoundAt = new ConcurrentHashMap<>();
    private final Map<UUID, ChatMode> playerChatModes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> worldMapActiveUntil = new ConcurrentHashMap<>();
    private FactionsPlugin plugin;
    private FactionWorldMapProvider worldMapProvider;
    private ScheduledFuture<?> claimTask;
    private ScheduledFuture<?> warTickTask;

    public HytaleFactionsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        try {
            plugin = new FactionsPlugin(getDataDirectory(), serverAdapter);
            plugin.setChatToggleHandler(playerId -> {
                toggleChatMode(playerId);
                return getChatMode(playerId).name();
            });
            plugin.setBorderToggleHandler(this::toggleBorders);
            plugin.setWorldMapToggleHandler(this::enableWorldMap);
            plugin.setClaimEffectHandler(this::handleClaimEffect);
            plugin.setFactionCreateHandler(this::handleFactionCreate);
            plugin.setWarDeclareHandler(this::handleWarDeclare);
            plugin.setMemberRoleChangeHandler(this::handleMemberRoleChange);
            plugin.combatService().setWarEndHandler(this::handleWarEnd);
            worldMapProvider = new FactionWorldMapProvider(plugin, serverAdapter::nowEpochMs, worldMapActiveUntil);
            registerWorldMapProviders();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize factions plugin", e);
        }

        getCommandRegistry().registerCommand(new FactionsCommand(this));
        getEventRegistry().register(ShutdownEvent.class, event -> {
            stopClaimScanner();
            stopWarTicker();
            flush();
        });
        getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::handlePlayerConnect);
        getEventRegistry().register(PlayerMouseButtonEvent.class, this::handleMouseEvent);
        getEventRegistry().register(PlayerChatEvent.class, "chat", this::handleChat);
        getEventRegistry().register(KillFeedEvent.KillerMessage.class, this::handleKillFeed);

        // Block protection events (ECS - more reliable than MouseButtonEvent)
        getEventRegistry().registerGlobal(BreakBlockEvent.class, this::handleBreakBlock);
        getEventRegistry().registerGlobal(PlaceBlockEvent.class, this::handlePlaceBlock);

        // FancyCore Integration
        try {
            Class.forName("com.fancyinnovations.fancycore.main.FancyCorePlugin");
            com.fancyinnovations.fancycore.api.placeholders.PlaceholderService.get()
                .registerProvider(new com.kingc.hytale.factions.integration.FactionNamePlaceholder(() -> plugin));
            registerFancyCoreEvents();
            LOGGER.atInfo().log("Linked with FancyCore (placeholders + warps).");
        } catch (Throwable e) {
            LOGGER.atInfo().log("FancyCore not found (or error linking), placeholders disabled.");
        }

        startClaimScanner();
        startWarTicker();
        LOGGER.atInfo().log("Loaded " + getName() + " v" + getManifest().getVersion());
    }

    public FactionsApi api() {
        return plugin;
    }

    private String enableWorldMap(UUID playerId) {
        if (plugin == null || playerId == null) {
            return null;
        }
        int durationSeconds = Math.max(1, plugin.settings().worldMapDurationSeconds);
        long now = serverAdapter.nowEpochMs();
        worldMapActiveUntil.put(playerId, now + (durationSeconds * 1000L));
        return "World map enabled for " + durationSeconds + "s.";
    }

    public Result<Double> depositTreasury(UUID playerId, double amount) {
        if (plugin == null) {
            return Result.error("Plugin not ready.");
        }
        if (amount <= 0) {
            return Result.error("Amount must be positive.");
        }
        if (!FancyCoreBridge.isAvailable()) {
            return Result.error("FancyCore economy not available.");
        }
        Optional<Currency> currencyOpt = FancyCoreBridge.getPrimaryCurrency();
        if (currencyOpt.isEmpty()) {
            return Result.error("FancyCore currency not available.");
        }
        Optional<FancyPlayer> playerOpt = FancyCoreBridge.getFancyPlayer(playerId);
        if (playerOpt.isEmpty()) {
            return Result.error("Player not found.");
        }
        Currency currency = currencyOpt.get();
        FancyPlayer fancyPlayer = playerOpt.get();
        double balance = fancyPlayer.getData().getBalance(currency);
        if (balance < amount) {
            return Result.error("Not enough balance.");
        }
        fancyPlayer.getData().removeBalance(currency, amount);
        return plugin.depositTreasury(playerId, amount);
    }

    public Result<Double> withdrawTreasury(UUID playerId, double amount) {
        if (plugin == null) {
            return Result.error("Plugin not ready.");
        }
        if (amount <= 0) {
            return Result.error("Amount must be positive.");
        }
        if (!FancyCoreBridge.isAvailable()) {
            return Result.error("FancyCore economy not available.");
        }
        Optional<Currency> currencyOpt = FancyCoreBridge.getPrimaryCurrency();
        if (currencyOpt.isEmpty()) {
            return Result.error("FancyCore currency not available.");
        }
        Optional<FancyPlayer> playerOpt = FancyCoreBridge.getFancyPlayer(playerId);
        if (playerOpt.isEmpty()) {
            return Result.error("Player not found.");
        }
        Result<Double> result = plugin.withdrawTreasury(playerId, amount);
        if (!result.ok()) {
            return result;
        }
        FancyPlayer fancyPlayer = playerOpt.get();
        fancyPlayer.getData().addBalance(currencyOpt.get(), amount);
        return result;
    }

    private void registerWorldMapProviders() {
        if (worldMapProvider == null) {
            return;
        }
        for (World world : Universe.get().getWorlds().values()) {
            WorldMapManager manager = world.getWorldMapManager();
            if (manager != null) {
                manager.addMarkerProvider("factions", worldMapProvider);
            }
        }
    }

    private void handleKillFeed(KillFeedEvent.KillerMessage event) {
        if (plugin == null) {
            return;
        }
        Damage damage = event.getDamage();
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource)) {
            return;
        }

        Ref<EntityStore> killerRef = ((Damage.EntitySource) source).getRef();
        Ref<EntityStore> victimRef = event.getTargetRef();
        if (killerRef == null || !killerRef.isValid() || victimRef == null || !victimRef.isValid()) {
            return;
        }

        Store<EntityStore> killerStore = killerRef.getStore();
        Store<EntityStore> victimStore = victimRef.getStore();
        if (killerStore == null || victimStore == null) {
            return;
        }

        PlayerRef killerPlayer = killerStore.getComponent(killerRef, PlayerRef.getComponentType());
        PlayerRef victimPlayer = victimStore.getComponent(victimRef, PlayerRef.getComponentType());
        if (killerPlayer == null || victimPlayer == null) {
            return;
        }

        UUID killerId = killerPlayer.getUuid();
        UUID victimId = victimPlayer.getUuid();
        if (killerId == null || victimId == null) {
            return;
        }

        // Record the kill
        CombatService.KillResult result = plugin.onPlayerKill(killerId, victimId);

        if (!result.success()) {
            return;
        }

        // Send notifications if configured
        FactionSettings settings = plugin.settings();
        if (settings.warNotifyOnKill && result.warPoints() > 0) {
            // War kill notification
            String killerName = Universe.get().getPlayer(killerId) != null ?
                    Universe.get().getPlayer(killerId).getUsername() : killerId.toString().substring(0, 8);
            String victimName = Universe.get().getPlayer(victimId) != null ?
                    Universe.get().getPlayer(victimId).getUsername() : victimId.toString().substring(0, 8);

            // Get war status
            if (result.killerFaction() != null) {
                var warOpt = plugin.combatService().getActiveWar(result.killerFaction().id());
                if (warOpt.isPresent()) {
                    War war = warOpt.get();
                    String message = settings.warKillMessage
                            .replace("{killer}", killerName)
                            .replace("{victim}", victimName)
                            .replace("{attackerPoints}", String.valueOf(war.attackerPoints()))
                            .replace("{defenderPoints}", String.valueOf(war.defenderPoints()));

                    // Notify all participants
                    notifyWarParticipants(war, message);
                }
            }
        }
    }

    private void notifyWarParticipants(War war, String message) {
        // Notify attacker faction
        var attackerFaction = plugin.service().getFactionById(war.attackerFactionId());
        attackerFaction.ifPresent(faction -> {
            for (UUID memberId : faction.members().keySet()) {
                PlayerRef player = Universe.get().getPlayer(memberId);
                if (player != null) {
                    player.sendMessage(Message.raw(message).color(plugin.settings().colorEnemy));
                }
            }
        });

        // Notify defender faction
        var defenderFaction = plugin.service().getFactionById(war.defenderFactionId());
        defenderFaction.ifPresent(faction -> {
            for (UUID memberId : faction.members().keySet()) {
                PlayerRef player = Universe.get().getPlayer(memberId);
                if (player != null) {
                    player.sendMessage(Message.raw(message).color(plugin.settings().colorEnemy));
                }
            }
        });
    }

    private void registerFancyCoreEvents() {
        if (plugin == null || !FancyCoreBridge.isAvailable()) {
            return;
        }
        plugin.events().register(FactionHomeSetEvent.class,
                event -> FancyCoreBridge.upsertFactionHomeWarp(event.faction(), event.home()));
        plugin.events().register(FactionRenamedEvent.class, event -> {
            FancyCoreBridge.removeFactionHomeWarp(event.oldName());
            if (event.faction().home() != null) {
                FancyCoreBridge.upsertFactionHomeWarp(event.faction(), event.faction().home());
            }
        });
        plugin.events().register(FactionDisbandedEvent.class,
                event -> FancyCoreBridge.removeFactionHomeWarp(event.faction().name()));
    }

    private void startWarTicker() {
        if (warTickTask != null) {
            return;
        }
        // Tick wars every 10 seconds
        warTickTask = claimScanner.scheduleAtFixedRate(this::tickWars, 10, 10, TimeUnit.SECONDS);
    }

    private void stopWarTicker() {
        if (warTickTask != null) {
            warTickTask.cancel(true);
            warTickTask = null;
        }
    }

    private void tickWars() {
        if (plugin == null) {
            return;
        }
        plugin.tickWars();
    }

    private void handleChat(PlayerChatEvent event) {
        if (plugin == null || event.isCancelled()) {
            return;
        }
        PlayerRef player = event.getSender();
        if (player == null) {
            return;
        }
        UUID playerId = player.getUuid();
        Optional<Faction> factionOpt = plugin.service().findFactionByMember(playerId);
        
        // Check chat mode
        ChatMode mode = playerChatModes.getOrDefault(playerId, ChatMode.PUBLIC);
        
        if (mode == ChatMode.FACTION || mode == ChatMode.ALLY) {
            if (factionOpt.isEmpty()) {
                player.sendMessage(Message.raw("[Factions] You are not in a faction."));
                return;
            }
            Faction faction = factionOpt.get();
            String content = event.getContent();
            String prefix = mode == ChatMode.FACTION ? "[Faction] " : "[Ally] ";
            
            // Cancel public broadcast
            event.setCancelled(true);
            
            // Send to faction members
            for (UUID memberId : faction.members().keySet()) {
                PlayerRef member = Universe.get().getPlayer(memberId);
                if (member != null) {
                    member.sendMessage(Message.raw(prefix + player.getUsername() + ": " + content));
                }
            }
            
            // If ally mode, also send to allies
            if (mode == ChatMode.ALLY) {
                for (UUID allyId : faction.allies()) {
                    Optional<Faction> allyFaction = plugin.service().getFactionById(allyId);
                    if (allyFaction.isPresent()) {
                        for (UUID memberId : allyFaction.get().members().keySet()) {
                            PlayerRef member = Universe.get().getPlayer(memberId);
                            if (member != null) {
                                member.sendMessage(Message.raw(prefix + "[" + faction.name() + "] " + player.getUsername() + ": " + content));
                            }
                        }
                    }
                }
            }
        } else {
            // Public chat - just add faction prefix if in faction
            if (factionOpt.isPresent()) {
                String prefix = "[" + factionOpt.get().name() + "] ";
                event.setContent(prefix + event.getContent());
            }
        }
    }

    public void toggleChatMode(UUID playerId) {
        ChatMode current = playerChatModes.getOrDefault(playerId, ChatMode.PUBLIC);
        ChatMode next = switch (current) {
            case PUBLIC -> ChatMode.FACTION;
            case FACTION -> ChatMode.ALLY;
            case ALLY -> ChatMode.PUBLIC;
        };
        playerChatModes.put(playerId, next);
    }

    public ChatMode getChatMode(UUID playerId) {
        return playerChatModes.getOrDefault(playerId, ChatMode.PUBLIC);
    }

    private String toggleBorders(UUID playerId, Integer secondsOverride) {
        if (plugin == null || playerId == null) {
            return "Borders view not available.";
        }
        long now = serverAdapter.nowEpochMs();
        if (secondsOverride != null && secondsOverride <= 0) {
            borderViewUntil.remove(playerId);
            lastBorderParticleAt.remove(playerId);
            return "Border view disabled.";
        }
        if (secondsOverride != null && secondsOverride > 120) {
            secondsOverride = 120;
        }
        Long until = borderViewUntil.get(playerId);
        boolean active = until != null && until > now;
        if (active && secondsOverride == null) {
            borderViewUntil.remove(playerId);
            lastBorderParticleAt.remove(playerId);
            return "Border view disabled.";
        }
        int duration = secondsOverride != null ? secondsOverride : plugin.settings().borderViewDurationSeconds;
        if (duration <= 0) {
            borderViewUntil.remove(playerId);
            lastBorderParticleAt.remove(playerId);
            return "Border view disabled.";
        }
        borderViewUntil.put(playerId, now + duration * 1000L);
        return "Border view enabled for " + duration + "s.";
    }

    private void handleFactionCreate(UUID actorId, Location location, UUID factionId, String factionName) {
        if (plugin == null || location == null) {
            return;
        }
        FactionSettings settings = plugin.settings();
        World world = Universe.get().getWorld(location.world());
        if (world == null) {
            return;
        }
        String particleAsset = normalizeParticleAsset(settings.factionCreateParticleAsset);
        int count = Math.max(1, settings.factionCreateParticleCount);
        double y = location.y() + settings.factionCreateParticleHeightOffset;
        Vector3d position = new Vector3d(location.x(), y, location.z());
        world.execute(() -> ParticleUtil.spawnParticles(world, particleAsset, position, count));

        PlayerRef playerRef = Universe.get().getPlayer(actorId);
        if (playerRef != null) {
            String titleText = renderTemplate(settings.factionCreateTitle, factionName, settings.wildernessLabel, location.world());
            String subtitleText = renderTemplate(settings.factionCreateSubtitle, factionName, settings.wildernessLabel, location.world());
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    Message.raw(titleText).color(settings.colorOwn),
                    Message.raw(subtitleText).color(settings.colorOwn),
                    true,
                    EventTitleUtil.DEFAULT_ZONE,
                    settings.factionCreateTitleFadeIn,
                    settings.factionCreateTitleStay,
                    settings.factionCreateTitleFadeOut
            );
            plugin.service().recordNotification(actorId, NotificationType.MAJOR, titleText, subtitleText);
        }
    }

    private void handlePlayerConnect(PlayerConnectEvent event) {
        if (plugin == null || event == null) {
            return;
        }
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null) {
            return;
        }
        UUID playerId = playerRef.getUuid();
        if (playerId == null) {
            return;
        }
        Optional<Faction> faction = plugin.service().findFactionByMember(playerId);
        if (faction.isEmpty()) {
            return;
        }
        String name = playerRef.getUsername() == null ? "Un membre" : playerRef.getUsername();
        String title = "Faction";
        String message = name + " est en ligne";
        for (UUID memberId : faction.get().members().keySet()) {
            if (memberId.equals(playerId)) {
                continue;
            }
            PlayerRef memberRef = Universe.get().getPlayer(memberId);
            if (memberRef != null) {
                notifyPlayer(memberRef, NotificationType.MINOR, title, message, false);
            } else {
                plugin.service().recordNotification(memberId, NotificationType.MINOR, title, message);
            }
        }
    }

    private void handleClaimEffect(UUID actorId, Location location, ClaimChangeType type, Optional<UUID> ownerId) {
        if (plugin == null || location == null || type == null) {
            return;
        }
        FactionSettings settings = plugin.settings();
        String particleAsset = type == ClaimChangeType.CLAIM
                ? normalizeParticleAsset(settings.claimParticleAsset)
                : normalizeParticleAsset(settings.unclaimParticleAsset);
        int count = Math.max(1, settings.claimParticleCount);
        double y = location.y() + settings.claimParticleHeightOffset;
        ClaimKey claim = ClaimKey.fromLocation(location, settings.chunkSize);
        double centerX = claim.x() * settings.chunkSize + (settings.chunkSize / 2.0);
        double centerZ = claim.z() * settings.chunkSize + (settings.chunkSize / 2.0);
        World world = Universe.get().getWorld(location.world());
        if (world == null) {
            return;
        }
        boolean isClaim = type == ClaimChangeType.CLAIM;
        Optional<Faction> actorFaction = isClaim ? plugin.service().findFactionByMember(actorId) : Optional.empty();
        Optional<War> warOpt = actorFaction.isPresent() ? plugin.combatService().getActiveWar(actorFaction.get().id()) : Optional.empty();
        boolean conquestActive = warOpt.isPresent() && warOpt.get().state() == War.WarState.ACTIVE;

        Vector3d position = new Vector3d(centerX, y, centerZ);
        String conquestAsset = normalizeParticleAsset(settings.conquestParticleAsset);
        int conquestCount = Math.max(1, settings.conquestParticleCount);
        double conquestY = location.y() + settings.conquestParticleHeightOffset;

        world.execute(() -> {
            ParticleUtil.spawnParticles(world, particleAsset, position, count);
            if (isClaim) {
                spawnClaimPillar(world, settings, centerX, location.y(), centerZ);
                if (conquestActive) {
                    Vector3d conquestPos = new Vector3d(centerX, conquestY, centerZ);
                    ParticleUtil.spawnParticles(world, conquestAsset, conquestPos, conquestCount);
                }
            }
        });

        if (isClaim && conquestActive && actorFaction.isPresent()) {
            PlayerRef playerRef = Universe.get().getPlayer(actorId);
            if (playerRef != null) {
                String titleText = renderTemplate(settings.conquestTitle, actorFaction.get().name(), settings.wildernessLabel, location.world());
                String subtitleText = renderTemplate(settings.conquestSubtitle, actorFaction.get().name(), settings.wildernessLabel, location.world());
                EventTitleUtil.showEventTitleToPlayer(
                        playerRef,
                        Message.raw(titleText).color(settings.colorEnemy),
                        Message.raw(subtitleText).color(settings.colorEnemy),
                        true,
                        EventTitleUtil.DEFAULT_ZONE,
                        settings.conquestTitleFadeIn,
                        settings.conquestTitleStay,
                        settings.conquestTitleFadeOut
                );
                plugin.service().recordNotification(actorId, NotificationType.WAR, titleText, subtitleText);
            }
        }
    }

    private void handleWarEnd(War war) {
        if (plugin == null || war == null) {
            return;
        }
        UUID winnerId = switch (war.result()) {
            case ATTACKER_WIN -> war.attackerFactionId();
            case DEFENDER_WIN -> war.defenderFactionId();
            default -> null;
        };
        if (winnerId == null) {
            return;
        }
        Optional<Faction> winnerFaction = plugin.service().getFactionById(winnerId);
        if (winnerFaction.isEmpty()) {
            return;
        }
        FactionSettings settings = plugin.settings();
        String titleText = renderTemplate(settings.warVictoryTitle, winnerFaction.get().name(), settings.wildernessLabel, "");
        String subtitleText = renderTemplate(settings.warVictorySubtitle, winnerFaction.get().name(), settings.wildernessLabel, "");
        String particleAsset = normalizeParticleAsset(settings.warVictoryParticleAsset);
        int count = Math.max(1, settings.warVictoryParticleCount);
        float heightOffset = settings.warVictoryParticleHeightOffset;

        for (UUID memberId : winnerFaction.get().members().keySet()) {
            PlayerRef playerRef = Universe.get().getPlayer(memberId);
            if (playerRef == null) {
                continue;
            }
            spawnParticlesAtPlayer(playerRef, particleAsset, count, heightOffset);
            playSoundToPlayer(playerRef, settings.soundWarVictory, settings);
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    Message.raw(titleText).color(settings.colorOwn),
                    Message.raw(subtitleText).color(settings.colorOwn),
                    true,
                    EventTitleUtil.DEFAULT_ZONE,
                    settings.warVictoryTitleFadeIn,
                    settings.warVictoryTitleStay,
                    settings.warVictoryTitleFadeOut
            );
            plugin.service().recordNotification(memberId, NotificationType.WAR, titleText, subtitleText);
        }
    }

    private void handleWarDeclare(UUID actorId, War war) {
        if (plugin == null || war == null) {
            return;
        }
        FactionSettings settings = plugin.settings();
        playSoundToFaction(war.attackerFactionId(), settings.soundWarDeclare, settings);
        playSoundToFaction(war.defenderFactionId(), settings.soundWarDeclare, settings);
        String attackerName = plugin.service().getFactionById(war.attackerFactionId()).map(Faction::name).orElse("Attaquant");
        String defenderName = plugin.service().getFactionById(war.defenderFactionId()).map(Faction::name).orElse("Defenseur");
        String title = "Guerre declaree";
        String message = attackerName + " vs " + defenderName;
        notifyFactionMembers(war.attackerFactionId(), NotificationType.WAR, title, message, true);
        notifyFactionMembers(war.defenderFactionId(), NotificationType.WAR, title, message, true);
    }

    private void handleMemberRoleChange(UUID actorId, UUID targetId, MemberRole newRole, boolean promoted) {
        if (plugin == null || targetId == null) {
            return;
        }
        PlayerRef targetRef = Universe.get().getPlayer(targetId);
        if (targetRef == null) {
            return;
        }
        FactionSettings settings = plugin.settings();
        String asset = promoted ? settings.soundRolePromote : settings.soundRoleDemote;
        playSoundToPlayer(targetRef, asset, settings);
        String title = promoted ? "Promotion" : "Retrogradation";
        String message = newRole == null ? "" : newRole.name().toLowerCase(Locale.ROOT);
        notifyPlayer(targetRef, NotificationType.ROLE, title, message, false);
    }

    private void playInvasionSound(UUID factionId, FactionSettings settings) {
        if (factionId == null) {
            return;
        }
        long now = serverAdapter.nowEpochMs();
        int cooldownSeconds = Math.max(1, settings.soundInvasionCooldownSeconds);
        Long last = lastInvasionSoundAt.get(factionId);
        if (last != null && now - last < cooldownSeconds * 1000L) {
            return;
        }
        lastInvasionSoundAt.put(factionId, now);
        playSoundToFaction(factionId, settings.soundTerritoryInvasion, settings);
        notifyFactionMembers(factionId, NotificationType.TERRITORY, "Invasion", "Ennemi detecte sur votre territoire", false);
    }

    private void playSoundToFaction(UUID factionId, String soundAsset, FactionSettings settings) {
        if (factionId == null) {
            return;
        }
        if (plugin == null) {
            return;
        }
        Optional<Faction> faction = plugin.service().getFactionById(factionId);
        if (faction.isEmpty()) {
            return;
        }
        for (UUID memberId : faction.get().members().keySet()) {
            PlayerRef playerRef = Universe.get().getPlayer(memberId);
            if (playerRef != null) {
                playSoundToPlayer(playerRef, soundAsset, settings);
            }
        }
    }

    private void notifyFactionMembers(UUID factionId, NotificationType type, String title, String message, boolean fullScreen) {
        if (plugin == null || factionId == null) {
            return;
        }
        Optional<Faction> faction = plugin.service().getFactionById(factionId);
        if (faction.isEmpty()) {
            return;
        }
        for (UUID memberId : faction.get().members().keySet()) {
            PlayerRef playerRef = Universe.get().getPlayer(memberId);
            if (playerRef != null) {
                notifyPlayer(playerRef, type, title, message, fullScreen);
            } else {
                plugin.service().recordNotification(memberId, type, title, message);
            }
        }
    }

    private void playSoundToPlayer(PlayerRef playerRef, String soundAsset, FactionSettings settings) {
        if (playerRef == null || soundAsset == null || soundAsset.isBlank()) {
            return;
        }
        int soundId = resolveSoundEventId(soundAsset);
        if (soundId == SoundEvent.EMPTY_ID) {
            return;
        }
        float volume = settings.soundVolume;
        float pitch = settings.soundPitch;
        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundId, SoundCategory.SFX, volume, pitch);
    }

    private int resolveSoundEventId(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return SoundEvent.EMPTY_ID;
        }
        return SoundEvent.getAssetMap().getIndexOrDefault(assetId, SoundEvent.EMPTY_ID);
    }

    private void notifyPlayer(PlayerRef playerRef, NotificationType type, String title, String message, boolean fullScreen) {
        if (playerRef == null || plugin == null) {
            return;
        }
        UUID playerId = playerRef.getUuid();
        if (playerId == null) {
            return;
        }
        plugin.service().recordNotification(playerId, type, title, message);
        if (!plugin.service().isNotificationsEnabled(playerId)) {
            return;
        }
        String safeTitle = title == null ? "" : title;
        String safeMessage = message == null ? "" : message;
        if (fullScreen) {
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    Message.raw(safeTitle),
                    Message.raw(safeMessage),
                    true,
                    EventTitleUtil.DEFAULT_ZONE,
                    0.3f,
                    2.5f,
                    0.5f
            );
            return;
        }
        PacketHandler handler = playerRef.getPacketHandler();
        if (handler == null) {
            return;
        }
        NotificationUtil.sendNotification(handler, Message.raw(safeTitle), Message.raw(safeMessage), NotificationStyle.Default);
    }



    boolean handleCommand(CommandContext ctx) {
        if (plugin == null) {
            return false;
        }
        String input = ctx.getInputString();
        String trimmed = input == null ? "" : input.trim();
        if (ctx.isPlayer() && isMenuCommand(trimmed)) {
            PlayerRef playerRef = ctx.sender() == null ? null : Universe.get().getPlayer(ctx.sender().getUuid());
            if (playerRef != null) {
                openMenu(playerRef);
                return true;
            }
        }
        return plugin.onCommand(new HytaleCommandSource(ctx), input);
    }

    void flush() {
        if (plugin == null) {
            return;
        }
        try {
            plugin.save();
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to save factions data");
        }
    }

    private void startClaimScanner() {
        if (claimTask != null) {
            return;
        }
        claimTask = claimScanner.scheduleAtFixedRate(this::scanClaims, 1, 1, TimeUnit.SECONDS);
    }

    private void stopClaimScanner() {
        if (claimTask != null) {
            claimTask.cancel(true);
            claimTask = null;
        }
        claimScanner.shutdownNow();
        lastClaimByPlayer.clear();
        lastClaimNotifyAt.clear();
        borderViewUntil.clear();
        lastBorderParticleAt.clear();
        lastInvasionSoundAt.clear();
    }

    private void scanClaims() {
        if (plugin == null) {
            return;
        }
        Set<UUID> online = new HashSet<>();
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            UUID playerId = playerRef.getUuid();
            if (playerId == null) {
                continue;
            }
            online.add(playerId);
            World world = Universe.get().getWorld(playerRef.getWorldUuid());
            if (world == null) {
                continue;
            }
            world.execute(() -> scanPlayerClaim(playerRef));
        }
        lastClaimByPlayer.keySet().removeIf(id -> !online.contains(id));
        lastClaimNotifyAt.keySet().removeIf(id -> !online.contains(id));
        borderViewUntil.keySet().removeIf(id -> !online.contains(id));
        lastBorderParticleAt.keySet().removeIf(id -> !online.contains(id));
    }

    private void scanPlayerClaim(PlayerRef playerRef) {
        Location location = toLocation(playerRef);
        if (location == null || plugin == null) {
            return;
        }
        FactionSettings settings = plugin.settings();
        renderBorderParticles(playerRef, location, settings);
        Optional<UUID> ownerId = plugin.service().getClaimOwnerId(location);
        String areaKey = ownerId.map(UUID::toString).orElse(WILDERNESS_KEY);
        UUID playerId = playerRef.getUuid();
        if (playerId == null) {
            return;
        }
        String lastKey = lastClaimByPlayer.put(playerId, areaKey);
        if (lastKey == null || lastKey.equals(areaKey)) {
            return;
        }
        boolean nowWild = WILDERNESS_KEY.equals(areaKey);
        if (nowWild && !Boolean.TRUE.equals(settings.notifyOnLeave)) {
            return;
        }
        if (!nowWild && !Boolean.TRUE.equals(settings.notifyOnEnter)) {
            return;
        }
        if (!canNotify(playerId, settings)) {
            return;
        }
        showClaimNotice(playerRef, location, ownerId, nowWild, settings);
        lastClaimNotifyAt.put(playerId, serverAdapter.nowEpochMs());
    }

    private boolean canNotify(UUID playerId, FactionSettings settings) {
        int cooldownSeconds = settings.notifyCooldownSeconds;
        if (cooldownSeconds <= 0) {
            return true;
        }
        long now = serverAdapter.nowEpochMs();
        Long last = lastClaimNotifyAt.get(playerId);
        if (last == null) {
            return true;
        }
        return now - last >= cooldownSeconds * 1000L;
    }

    private void showClaimNotice(PlayerRef playerRef, Location location, Optional<UUID> ownerId, boolean nowWild, FactionSettings settings) {
        boolean useTitle = Boolean.TRUE.equals(settings.notifyUseTitle);
        boolean useChat = Boolean.TRUE.equals(settings.notifyUseChat);
        if (!useTitle && !useChat) {
            return;
        }

        UUID playerId = playerRef.getUuid();
        String factionName;
        String color;
        String relationPrefix = "";
        boolean isEnemy = false;

        if (nowWild) {
            factionName = settings.wildernessLabel;
            color = settings.colorWilderness;
            relationPrefix = "";
        } else if (ownerId.isPresent()) {
            UUID claimOwnerId = ownerId.get();
            Optional<Faction> claimFaction = plugin.service().getFactionById(claimOwnerId);
            factionName = claimFaction.map(Faction::name).orElse("Faction inconnue");

            // Déterminer la relation
            Optional<Faction> playerFaction = plugin.service().findFactionByMember(playerId);
            if (playerFaction.isPresent()) {
                Faction myFaction = playerFaction.get();
                if (myFaction.id().equals(claimOwnerId)) {
                    // Notre propre faction
                    color = settings.colorOwn;
                    relationPrefix = "[Votre Territoire] ";
                } else if (myFaction.allies().contains(claimOwnerId)) {
                    // Faction alliée
                    color = settings.colorAlly;
                    relationPrefix = "[Allié] ";
                } else if (myFaction.enemies().contains(claimOwnerId)) {
                    // Faction ennemie
                    color = settings.colorEnemy;
                    relationPrefix = "[Ennemi] ";
                    isEnemy = true;
                } else {
                    // Faction neutre
                    color = settings.colorNeutral;
                    relationPrefix = "[Neutre] ";
                }
                if (!isEnemy && claimFaction.isPresent() && claimFaction.get().enemies().contains(myFaction.id())) {
                    isEnemy = true;
                }
            } else {
                // Joueur sans faction - tout est neutre
                color = settings.colorNeutral;
                relationPrefix = "";
            }
        } else {
            factionName = settings.wildernessLabel;
            color = settings.colorWilderness;
            relationPrefix = "";
        }

        String worldName = location.world();
        String titleTemplate = nowWild ? settings.claimLeaveTitle : settings.claimEnterTitle;
        String subtitleTemplate = nowWild ? settings.claimLeaveSubtitle : settings.claimEnterSubtitle;
        String titleText = renderTemplate(titleTemplate, factionName, settings.wildernessLabel, worldName);
        String subtitleText = renderTemplate(subtitleTemplate, factionName, settings.wildernessLabel, worldName);

        if (useChat) {
            String chat = subtitleText == null || subtitleText.isBlank()
                    ? relationPrefix + titleText
                    : relationPrefix + titleText + ": " + subtitleText;
            playerRef.sendMessage(Message.raw("[Factions] " + chat).color(color));
        }
        if (useTitle) {
            Message title = Message.raw(titleText).color(color);
            Message subtitle = Message.raw(relationPrefix + subtitleText).color(color);
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    title,
                    subtitle,
                    true,
                    EventTitleUtil.DEFAULT_ZONE,
                    settings.claimTitleFadeIn,
                    settings.claimTitleStay,
                    settings.claimTitleFadeOut
            );
        }

        if (nowWild) {
            playSoundToPlayer(playerRef, settings.soundTerritoryLeave, settings);
        } else {
            playSoundToPlayer(playerRef, settings.soundTerritoryEnter, settings);
            if (isEnemy && ownerId.isPresent()) {
                playInvasionSound(ownerId.get(), settings);
            }
        }

        String noticeTitle = "Territoire";
        String noticeMessage = nowWild ? settings.wildernessLabel : factionName;
        notifyPlayer(playerRef, NotificationType.TERRITORY, noticeTitle, noticeMessage, false);
    }

    private String renderTemplate(String template, String factionName, String wildernessName, String worldName) {
        if (template == null) {
            return "";
        }
        String text = template;
        text = text.replace("{faction}", factionName == null ? "" : factionName);
        text = text.replace("{wilderness}", wildernessName == null ? "" : wildernessName);
        text = text.replace("{world}", worldName == null ? "" : worldName);
        return text;
    }

    private void spawnClaimPillar(World world, FactionSettings settings, double centerX, double baseY, double centerZ) {
        int height = Math.max(1, settings.claimPillarHeight);
        int step = Math.max(1, settings.claimPillarStep);
        int count = Math.max(1, settings.claimPillarParticleCount);
        String asset = normalizeParticleAsset(settings.claimPillarParticleAsset);
        for (int offset = 0; offset <= height; offset += step) {
            Vector3d position = new Vector3d(centerX, baseY + offset, centerZ);
            ParticleUtil.spawnParticles(world, asset, position, count);
        }
    }

    private void spawnParticlesAtPlayer(PlayerRef playerRef, String particleAsset, int count, float heightOffset) {
        Transform transform = playerRef.getTransform();
        if (transform == null) {
            return;
        }
        Vector3d position = transform.getPosition();
        if (position == null) {
            return;
        }
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return;
        }
        Vector3d spawnPos = new Vector3d(position.getX(), position.getY() + heightOffset, position.getZ());
        world.execute(() -> ParticleUtil.spawnParticles(world, particleAsset, spawnPos, count));
    }

    private Location toLocation(PlayerRef playerRef) {
        Transform transform = playerRef.getTransform();
        if (transform == null) {
            return null;
        }
        Vector3d position = transform.getPosition();
        Vector3f rotation = transform.getRotation();
        if (position == null || rotation == null) {
            return null;
        }
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        String worldName = world != null ? world.getName() : playerRef.getWorldUuid().toString();
        return new Location(worldName, position.getX(), position.getY(), position.getZ(), rotation.getYaw(), rotation.getPitch());
    }

    private void renderBorderParticles(PlayerRef playerRef, Location location, FactionSettings settings) {
        UUID playerId = playerRef.getUuid();
        if (playerId == null || plugin == null) {
            return;
        }
        long now = serverAdapter.nowEpochMs();
        Long until = borderViewUntil.get(playerId);
        if (until == null || until <= now) {
            borderViewUntil.remove(playerId);
            return;
        }
        int intervalSeconds = Math.max(1, settings.borderParticleIntervalSeconds);
        Long lastAt = lastBorderParticleAt.get(playerId);
        if (lastAt != null && now - lastAt < intervalSeconds * 1000L) {
            return;
        }
        lastBorderParticleAt.put(playerId, now);

        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return;
        }

        ClaimKey claim = ClaimKey.fromLocation(location, settings.chunkSize);
        Optional<UUID> currentOwner = plugin.service().getClaimOwnerId(claim);
        Optional<Faction> playerFaction = plugin.service().findFactionByMember(playerId);

        int chunkSize = settings.chunkSize;
        int startX = claim.x() * chunkSize;
        int startZ = claim.z() * chunkSize;
        int endX = startX + chunkSize;
        int endZ = startZ + chunkSize;
        int step = Math.max(1, settings.borderParticleStep);
        int count = Math.max(1, settings.borderParticleCount);
        double y = location.y() + settings.borderParticleHeightOffset;

        spawnBorderEdge(world, currentOwner, new ClaimKey(claim.world(), claim.x() - 1, claim.z()),
                startX, startZ, startX, endZ, y, step, count, playerFaction, settings);
        spawnBorderEdge(world, currentOwner, new ClaimKey(claim.world(), claim.x() + 1, claim.z()),
                endX, startZ, endX, endZ, y, step, count, playerFaction, settings);
        spawnBorderEdge(world, currentOwner, new ClaimKey(claim.world(), claim.x(), claim.z() - 1),
                startX, startZ, endX, startZ, y, step, count, playerFaction, settings);
        spawnBorderEdge(world, currentOwner, new ClaimKey(claim.world(), claim.x(), claim.z() + 1),
                startX, endZ, endX, endZ, y, step, count, playerFaction, settings);
    }

    private void spawnBorderEdge(World world, Optional<UUID> currentOwner, ClaimKey neighbor,
                                 int startX, int startZ, int endX, int endZ, double y,
                                 int step, int count, Optional<Faction> playerFaction, FactionSettings settings) {
        Optional<UUID> neighborOwner = plugin.service().getClaimOwnerId(neighbor);
        if (Objects.equals(currentOwner.orElse(null), neighborOwner.orElse(null))) {
            return;
        }
        Optional<UUID> edgeOwner = currentOwner.isPresent() ? currentOwner : neighborOwner;
        if (edgeOwner.isEmpty()) {
            return;
        }
        String asset = resolveBorderParticleAsset(playerFaction, edgeOwner.get(), settings);
        spawnBorderLine(world, asset, y, startX, startZ, endX, endZ, step, count);
    }

    private String resolveBorderParticleAsset(Optional<Faction> playerFaction, UUID ownerId, FactionSettings settings) {
        if (ownerId == null) {
            return normalizeParticleAsset(settings.borderParticleWilderness);
        }
        if (playerFaction.isPresent()) {
            Faction faction = playerFaction.get();
            if (faction.id().equals(ownerId)) {
                return normalizeParticleAsset(settings.borderParticleOwn);
            }
            if (faction.allies().contains(ownerId)) {
                return normalizeParticleAsset(settings.borderParticleAlly);
            }
            if (faction.enemies().contains(ownerId)) {
                return normalizeParticleAsset(settings.borderParticleEnemy);
            }
        }
        return normalizeParticleAsset(settings.borderParticleNeutral);
    }

    private void spawnBorderLine(World world, String asset, double y,
                                 int startX, int startZ, int endX, int endZ, int step, int count) {
        String particleAsset = normalizeParticleAsset(asset);
        if (startX == endX) {
            int x = startX;
            for (int z = startZ; z <= endZ; z += step) {
                Vector3d position = new Vector3d(x + 0.5, y, z + 0.5);
                ParticleUtil.spawnParticles(world, particleAsset, position, count);
            }
            return;
        }
        if (startZ == endZ) {
            int z = startZ;
            for (int x = startX; x <= endX; x += step) {
                Vector3d position = new Vector3d(x + 0.5, y, z + 0.5);
                ParticleUtil.spawnParticles(world, particleAsset, position, count);
            }
        }
    }

    private String normalizeParticleAsset(String asset) {
        if (asset == null || asset.isBlank()) {
            return DEFAULT_PARTICLE_ASSET;
        }
        return asset;
    }

    // ==================== BLOCK PROTECTION (ECS Events) ====================

    private void handleBreakBlock(BreakBlockEvent event) {
        if (plugin == null || event.isCancelled()) {
            return;
        }

        UUID playerId = event.getPlayerUuid();
        if (playerId == null) {
            return;
        }

        Vector3i blockPos = event.getBlockPosition();
        if (blockPos == null) {
            return;
        }

        World world = event.getWorld();
        String worldName = world != null ? world.getName() : "unknown";
        Location location = new Location(worldName, blockPos.getX(), blockPos.getY(), blockPos.getZ(), 0f, 0f);

        if (!plugin.canBuild(new HytalePlayerRef(playerId), location)) {
            event.setCancelled(true);
            PlayerRef playerRef = Universe.get().getPlayer(playerId);
            if (playerRef != null) {
                playerRef.sendMessage(Message.raw("[Factions] You cannot break blocks here.").color(plugin.settings().colorEnemy));
            }
        }
    }

    private void handlePlaceBlock(PlaceBlockEvent event) {
        if (plugin == null || event.isCancelled()) {
            return;
        }

        UUID playerId = event.getPlayerUuid();
        if (playerId == null) {
            return;
        }

        Vector3i blockPos = event.getBlockPosition();
        if (blockPos == null) {
            return;
        }

        World world = event.getWorld();
        String worldName = world != null ? world.getName() : "unknown";
        Location location = new Location(worldName, blockPos.getX(), blockPos.getY(), blockPos.getZ(), 0f, 0f);

        if (!plugin.canBuild(new HytalePlayerRef(playerId), location)) {
            event.setCancelled(true);
            PlayerRef playerRef = Universe.get().getPlayer(playerId);
            if (playerRef != null) {
                playerRef.sendMessage(Message.raw("[Factions] You cannot place blocks here.").color(plugin.settings().colorEnemy));
            }
        }
    }

    // ==================== LEGACY MOUSE EVENT (for PvP protection) ====================

    private void handleMouseEvent(PlayerMouseButtonEvent event) {
        if (plugin == null || event.isCancelled()) {
            return;
        }
        PlayerRef attacker = event.getPlayerRefComponent();
        if (attacker == null) {
            return;
        }

        // Block protection is now handled by BreakBlockEvent and PlaceBlockEvent
        // This handler is kept for potential future PvP protection

        // TODO: Friendly fire protection disabled due to Java 25 compiler bug with Entity.getUuid()
        // Uncomment when Hytale provides an alternative API or Java fixes the bug
        // Entity targetEntity = event.getTargetEntity();
        // if (targetEntity instanceof Player targetPlayer) {
        //     UUID targetId = targetPlayer.getUuid();
        //     if (targetId == null) {
        //         return;
        //     }
        //     UUID attackerId = attacker.getUuid();
        //     if (!plugin.service().canDamage(attackerId, targetId)) {
        //         event.setCancelled(true);
        //         attacker.sendMessage(Message.raw("[Factions] Friendly fire is disabled."));
        //     }
        // }
    }

    private Location toBlockLocation(PlayerRef playerRef, Vector3i block) {
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        String worldName = world != null ? world.getName() : playerRef.getWorldUuid().toString();
        return new Location(worldName, block.getX(), block.getY(), block.getZ(), 0f, 0f);
    }

    FactionsPlugin core() {
        return plugin;
    }

    void openMenu(PlayerRef playerRef) {
        if (playerRef == null || plugin == null) {
            return;
        }
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            playerRef.sendMessage(Message.raw("[Factions] World unavailable."));
            return;
        }
        world.execute(() -> {
            EntityStore entityStore = world.getEntityStore();
            if (entityStore == null) {
                return;
            }
            Store<EntityStore> store = entityStore.getStore();
            Ref<EntityStore> ref = entityStore.getRefFromUUID(playerRef.getUuid());
            if (store == null || ref == null) {
                return;
            }
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return;
            }
            player.getPageManager().openCustomPage(ref, store, new FactionsMenuPage(this, playerRef));
        });
    }

    private boolean isMenuCommand(String input) {
        if (input == null || input.isBlank()) {
            return true;
        }
        String[] parts = input.trim().toLowerCase(Locale.ROOT).split("\\s+");
        if (parts.length == 1) {
            return isMenuRoot(parts[0]);
        }
        if (parts.length == 2 && isMenuRoot(parts[0])) {
            return parts[1].equals("menu") || parts[1].equals("ui");
        }
        return false;
    }

    private boolean isMenuRoot(String token) {
        return token.equals("f") || token.equals("faction") || token.equals("factions");
    }
}
