package com.kingc.hytale.duels;

import com.kingc.hytale.duels.api.CommandSource;
import com.kingc.hytale.duels.api.Location;
import com.kingc.hytale.duels.api.ServerAdapter;
import com.kingc.hytale.duels.arena.ArenaRepository;
import com.kingc.hytale.duels.arena.ArenaService;
import com.kingc.hytale.duels.command.CommandDispatcher;
import com.kingc.hytale.duels.kit.KitRepository;
import com.kingc.hytale.duels.kit.KitService;
import com.kingc.hytale.duels.match.MatchService;
import com.kingc.hytale.duels.queue.QueueService;
import com.kingc.hytale.duels.ranking.RankingRepository;
import com.kingc.hytale.duels.ranking.RankingService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class DuelsPlugin {
    private final Path dataDir;
    private final ServerAdapter server;
    private final DuelsSettings settings;
    private final KitRepository kitRepository;
    private final KitService kitService;
    private final ArenaRepository arenaRepository;
    private final ArenaService arenaService;
    private final RankingRepository rankingRepository;
    private final RankingService rankingService;
    private final MatchService matchService;
    private final QueueService queueService;
    private final CommandDispatcher dispatcher;

    public DuelsPlugin(Path dataDir, ServerAdapter server) throws IOException {
        this.dataDir = dataDir;
        this.server = server;

        Files.createDirectories(dataDir);

        this.settings = DuelsSettings.load(dataDir.resolve("settings.json"));

        this.kitRepository = new KitRepository(dataDir.resolve("kits.json"));
        this.kitService = new KitService(kitRepository, server);

        this.arenaRepository = new ArenaRepository(dataDir.resolve("arenas.json"));
        this.arenaService = new ArenaService(arenaRepository);

        this.rankingRepository = new RankingRepository(dataDir.resolve("rankings.json"));
        this.rankingService = new RankingService(rankingRepository, server, server::nowEpochMs);

        this.matchService = new MatchService(server, arenaService, kitService, rankingService, server::nowEpochMs);
        this.queueService = new QueueService(server, matchService, server::nowEpochMs);

        this.dispatcher = new CommandDispatcher(matchService, queueService, kitService, rankingService, server);
    }

    public void onDisable() throws IOException {
        kitService.save();
        arenaService.save();
        rankingService.save();
        settings.save(dataDir.resolve("settings.json"));
    }

    public void save() throws IOException {
        kitService.save();
        arenaService.save();
        rankingService.save();
        settings.save(dataDir.resolve("settings.json"));
    }

    public boolean onCommand(CommandSource source, String commandLine) {
        return dispatcher.handle(source, commandLine);
    }

    public void onPlayerDeath(UUID playerId) {
        matchService.handlePlayerDeath(playerId);
    }

    public void setLobbySpawn(Location location) {
        settings.setLobbySpawn(location);
    }

    public DuelsSettings settings() {
        return settings;
    }

    public KitService kitService() {
        return kitService;
    }

    public ArenaService arenaService() {
        return arenaService;
    }

    public MatchService matchService() {
        return matchService;
    }

    public QueueService queueService() {
        return queueService;
    }

    public RankingService rankingService() {
        return rankingService;
    }
}
