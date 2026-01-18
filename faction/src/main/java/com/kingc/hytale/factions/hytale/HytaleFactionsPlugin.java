package com.kingc.hytale.factions.hytale;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ShutdownEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kingc.hytale.factions.FactionsPlugin;
import com.kingc.hytale.factions.api.Location;
import com.kingc.hytale.factions.model.Faction;
import com.kingc.hytale.factions.service.FactionSettings;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.kingc.hytale.factions.model.ChatMode;


import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
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

    private final HytaleServerAdapter serverAdapter = new HytaleServerAdapter();
    private final ScheduledExecutorService claimScanner = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "HytaleFactions-ClaimScanner");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<UUID, String> lastClaimByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastClaimNotifyAt = new ConcurrentHashMap<>();
    private final Map<UUID, ChatMode> playerChatModes = new ConcurrentHashMap<>();
    private FactionsPlugin plugin;
    private ScheduledFuture<?> claimTask;

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
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize factions plugin", e);
        }

        getCommandRegistry().registerCommand(new FactionsCommand(this));
        getEventRegistry().register(ShutdownEvent.class, event -> {
            stopClaimScanner();
            flush();
        });
        getEventRegistry().register(PlayerMouseButtonEvent.class, this::handleMouseEvent);
        getEventRegistry().register(PlayerChatEvent.class, "chat", this::handleChat);

        // FancyCore Integration
        try {
            Class.forName("com.fancyinnovations.fancycore.main.FancyCorePlugin");
            com.fancyinnovations.fancycore.api.placeholders.PlaceholderService.get()
                .registerProvider(new com.kingc.hytale.factions.integration.FactionNamePlaceholder(() -> plugin));
            LOGGER.atInfo().log("Linked with FancyCore for placeholders!");
        } catch (Throwable e) {
            LOGGER.atInfo().log("FancyCore not found (or error linking), placeholders disabled.");
        }

        startClaimScanner();
        LOGGER.atInfo().log("Loaded " + getName() + " v" + getManifest().getVersion());
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
    }

    private void scanPlayerClaim(PlayerRef playerRef) {
        Location location = toLocation(playerRef);
        if (location == null || plugin == null) {
            return;
        }
        FactionSettings settings = plugin.settings();
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
                } else {
                    // Faction neutre
                    color = settings.colorNeutral;
                    relationPrefix = "[Neutre] ";
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

    private void handleMouseEvent(PlayerMouseButtonEvent event) {
        if (plugin == null || event.isCancelled()) {
            return;
        }
        PlayerRef attacker = event.getPlayerRefComponent();
        if (attacker == null) {
            return;
        }

        Vector3i targetBlock = event.getTargetBlock();
        if (targetBlock != null) {
            Location location = toBlockLocation(attacker, targetBlock);
            if (location != null && !plugin.service().canBuild(attacker.getUuid(), location)) {
                event.setCancelled(true);
                attacker.sendMessage(Message.raw("[Factions] You cannot build here."));
                return;
            }
        }

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
