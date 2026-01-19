package com.kingc.hytale.factions;

import com.kingc.hytale.factions.api.ClaimEffectHandler;
import com.kingc.hytale.factions.api.CommandSource;
import com.kingc.hytale.factions.api.FactionCreateHandler;
import com.kingc.hytale.factions.api.FactionsApi;
import com.kingc.hytale.factions.api.Location;
import com.kingc.hytale.factions.api.MemberRoleChangeHandler;
import com.kingc.hytale.factions.api.PlayerRef;
import com.kingc.hytale.factions.api.ServerAdapter;
import com.kingc.hytale.factions.api.WarDeclareHandler;
import com.kingc.hytale.factions.api.event.FactionEventBus;
import com.kingc.hytale.factions.command.CommandDispatcher;
import com.kingc.hytale.factions.model.ClaimKey;
import com.kingc.hytale.factions.model.Faction;
import com.kingc.hytale.factions.model.War;
import com.kingc.hytale.factions.service.CombatService;
import com.kingc.hytale.factions.service.FactionService;
import com.kingc.hytale.factions.service.FactionSettings;
import com.kingc.hytale.factions.service.ActionLogger;
import com.kingc.hytale.factions.service.Result;
import com.kingc.hytale.factions.storage.CombatDataStore;
import com.kingc.hytale.factions.storage.FactionDataStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

public final class FactionsPlugin implements FactionsApi {
    private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path dataDir;
    private final Path configPath;
    private final ServerAdapter server;
    private final FactionSettings settings;
    private final FactionDataStore store;
    private final CombatDataStore combatStore;
    private final FactionService service;
    private final CombatService combatService;
    private final CommandDispatcher dispatcher;
    private final FactionEventBus eventBus;

    public FactionsPlugin(Path dataDir, ServerAdapter server) throws IOException {
        this.dataDir = dataDir;
        this.configPath = dataDir.resolve("config.json");
        this.server = server;

        Files.createDirectories(dataDir);
        this.settings = FactionSettings.load(configPath);
        this.store = new FactionDataStore(dataDir.resolve("factions.json"));
        this.combatStore = new CombatDataStore(dataDir.resolve("combat.json"));
        ActionLogger actionLogger = new ActionLogger(dataDir.resolve("logs").resolve("actions.log"));
        this.eventBus = new FactionEventBus();
        this.service = new FactionService(store, settings, server::nowEpochMs, actionLogger, eventBus);

        // Initialize combat service with settings
        CombatService.CombatSettings combatSettings = new CombatService.CombatSettings();
        combatSettings.warPointsPerKill = settings.warPointsPerKill;
        combatSettings.warPointsToWin = settings.warPointsToWin;
        combatSettings.warGracePeriodMinutes = settings.warGracePeriodMinutes;
        combatSettings.warDurationMinutes = settings.warDurationMinutes;
        combatSettings.warCooldownMinutes = settings.warCooldownMinutes;
        combatSettings.roleForWar = settings.roleForWar();
        this.combatService = new CombatService(combatStore, service, server::nowEpochMs, actionLogger, combatSettings, eventBus);

        this.dispatcher = new CommandDispatcher(service, combatService, server, settings, this::reloadSettings);
    }

    public void onDisable() throws IOException {
        store.save();
        combatStore.save();
    }

    public void save() throws IOException {
        store.save();
        combatStore.save();
    }

    public void saveWithBackup() throws IOException {
        store.save();
        combatStore.save();
        createBackup();
    }

    public Result<Void> reloadSettings() {
        try {
            FactionSettings loaded = FactionSettings.load(configPath);
            settings.applyFrom(loaded);
            return Result.ok("Config reloaded.", null);
        } catch (IOException e) {
            return Result.error("Failed to reload config: " + e.getMessage());
        }
    }

    public boolean onCommand(CommandSource source, String commandLine) {
        return dispatcher.handle(source, commandLine);
    }

    public boolean canBuild(PlayerRef player, Location location) {
        return service.canBuild(player.id(), location);
    }

    public boolean canDamage(PlayerRef attacker, PlayerRef target) {
        return service.canDamage(attacker.id(), target.id());
    }

    public FactionService service() {
        return service;
    }

    public CombatService combatService() {
        return combatService;
    }

    public FactionSettings settings() {
        return settings;
    }

    public Result<Double> depositTreasury(UUID actorId, double amount) {
        return service.depositTreasury(actorId, amount);
    }

    public Result<Double> withdrawTreasury(UUID actorId, double amount) {
        return service.withdrawTreasury(actorId, amount);
    }

    public Result<Double> adjustTreasury(UUID factionId, UUID actorId, double amount) {
        return service.adjustTreasury(factionId, actorId, amount);
    }

    @Override
    public Optional<Faction> getFactionById(UUID factionId) {
        return service.getFactionById(factionId);
    }

    @Override
    public Optional<Faction> getFactionByName(String name) {
        return service.findFactionByName(name);
    }

    @Override
    public Optional<Faction> getFactionByMember(UUID playerId) {
        return service.findFactionByMember(playerId);
    }

    @Override
    public Collection<Faction> getFactions() {
        return service.factions().values();
    }

    @Override
    public Optional<UUID> getClaimOwnerId(ClaimKey claim) {
        return service.getClaimOwnerId(claim);
    }

    @Override
    public Optional<UUID> getClaimOwnerId(Location location) {
        return service.getClaimOwnerId(location);
    }

    @Override
    public Optional<War> getActiveWar(UUID factionId) {
        return combatService.getActiveWar(factionId);
    }

    @Override
    public List<War> getWarHistory() {
        return combatService.getWarHistory();
    }

    @Override
    public Optional<Double> getTreasuryBalance(UUID factionId) {
        return service.getTreasuryBalance(factionId);
    }

    @Override
    public FactionEventBus events() {
        return eventBus;
    }

    /**
     * Enregistre un kill entre deux joueurs. Appelé par l'adaptateur Hytale.
     */
    public CombatService.KillResult onPlayerKill(UUID killerId, UUID victimId) {
        return combatService.recordKill(killerId, victimId);
    }

    /**
     * Met à jour les guerres actives. Appelé périodiquement.
     */
    public void tickWars() {
        combatService.tickWars();
    }

    public void setChatToggleHandler(java.util.function.Function<java.util.UUID, String> handler) {
        dispatcher.setChatToggleHandler(handler);
    }

    public void setBorderToggleHandler(BiFunction<UUID, Integer, String> handler) {
        dispatcher.setBorderToggleHandler(handler);
    }

    public void setWorldMapToggleHandler(java.util.function.Function<UUID, String> handler) {
        dispatcher.setWorldMapToggleHandler(handler);
    }

    public void setClaimEffectHandler(ClaimEffectHandler handler) {
        dispatcher.setClaimEffectHandler(handler);
    }

    public void setFactionCreateHandler(FactionCreateHandler handler) {
        dispatcher.setFactionCreateHandler(handler);
    }

    public void setWarDeclareHandler(WarDeclareHandler handler) {
        dispatcher.setWarDeclareHandler(handler);
    }

    public void setMemberRoleChangeHandler(MemberRoleChangeHandler handler) {
        dispatcher.setMemberRoleChangeHandler(handler);
    }

    private void createBackup() throws IOException {
        if (settings.backupRetention <= 0) {
            return;
        }
        Path backupDir = dataDir.resolve("backups");
        Files.createDirectories(backupDir);
        String timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(server.nowEpochMs()), ZoneId.systemDefault())
                .format(BACKUP_FORMAT);
        Path factionsPath = dataDir.resolve("factions.json");
        Path backupPath = backupDir.resolve("factions-" + timestamp + ".json");
        if (Files.exists(factionsPath)) {
            Files.copy(factionsPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        }
        if (Files.exists(configPath)) {
            Path configBackup = backupDir.resolve("config-" + timestamp + ".json");
            Files.copy(configPath, configBackup, StandardCopyOption.REPLACE_EXISTING);
        }
        trimBackups(backupDir, settings.backupRetention);
    }

    private void trimBackups(Path backupDir, int keep) throws IOException {
        if (keep <= 0) {
            return;
        }
        List<Path> backups = new ArrayList<>();
        try (var stream = Files.list(backupDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(backups::add);
        }
        backups.sort(Comparator.comparingLong((Path path) -> path.toFile().lastModified()).reversed());
        for (int i = keep; i < backups.size(); i++) {
            Files.deleteIfExists(backups.get(i));
        }
    }
}
