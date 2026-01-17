package com.kingc.hytale.factions;

import com.kingc.hytale.factions.api.CommandSource;
import com.kingc.hytale.factions.api.Location;
import com.kingc.hytale.factions.api.PlayerRef;
import com.kingc.hytale.factions.api.ServerAdapter;
import com.kingc.hytale.factions.command.CommandDispatcher;
import com.kingc.hytale.factions.service.FactionService;
import com.kingc.hytale.factions.service.FactionSettings;
import com.kingc.hytale.factions.service.ActionLogger;
import com.kingc.hytale.factions.service.Result;
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
import java.util.Comparator;
import java.util.List;

public final class FactionsPlugin {
    private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path dataDir;
    private final Path configPath;
    private final ServerAdapter server;
    private final FactionSettings settings;
    private final FactionDataStore store;
    private final FactionService service;
    private final CommandDispatcher dispatcher;

    public FactionsPlugin(Path dataDir, ServerAdapter server) throws IOException {
        this.dataDir = dataDir;
        this.configPath = dataDir.resolve("config.json");
        this.server = server;

        Files.createDirectories(dataDir);
        this.settings = FactionSettings.load(configPath);
        this.store = new FactionDataStore(dataDir.resolve("factions.json"));
        ActionLogger actionLogger = new ActionLogger(dataDir.resolve("logs").resolve("actions.log"));
        this.service = new FactionService(store, settings, server::nowEpochMs, actionLogger);
        this.dispatcher = new CommandDispatcher(service, server, settings, this::reloadSettings);
    }

    public void onDisable() throws IOException {
        store.save();
    }

    public void save() throws IOException {
        store.save();
    }

    public void saveWithBackup() throws IOException {
        store.save();
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

    public FactionSettings settings() {
        return settings;
    }

    public void setChatToggleHandler(java.util.function.Function<java.util.UUID, String> handler) {
        dispatcher.setChatToggleHandler(handler);
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
