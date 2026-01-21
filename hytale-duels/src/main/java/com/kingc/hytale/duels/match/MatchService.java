package com.kingc.hytale.duels.match;

import com.kingc.hytale.duels.api.PlayerRef;
import com.kingc.hytale.duels.api.ServerAdapter;
import com.kingc.hytale.duels.api.event.DuelEventBus;
import com.kingc.hytale.duels.api.event.MatchEndedEvent;
import com.kingc.hytale.duels.api.event.MatchStartedEvent;
import com.kingc.hytale.duels.arena.Arena;
import com.kingc.hytale.duels.arena.ArenaService;
import com.kingc.hytale.duels.kit.KitDefinition;
import com.kingc.hytale.duels.kit.KitService;
import com.kingc.hytale.duels.notifications.NotificationService;
import com.kingc.hytale.duels.ranking.RankingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

public final class MatchService {
    private static final long REQUEST_COOLDOWN_MS = 5_000;
    private static final long MATCH_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes
    private static final long WARNING_TIME_MS = 4 * 60 * 1000;  // 4 minutes (1 min before end)

    private final ServerAdapter server;
    private final ArenaService arenaService;
    private final KitService kitService;
    private final RankingService rankingService;
    private final DuelEventBus eventBus;
    private final NotificationService notificationService;
    private final LongSupplier clock;

    private final Map<UUID, DuelRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastRequestTime = new ConcurrentHashMap<>();
    private final Map<String, Match> activeMatches = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerToMatch = new ConcurrentHashMap<>();
    private final AtomicInteger matchIdCounter = new AtomicInteger(1);
    private final java.util.Set<String> warnedMatches = ConcurrentHashMap.newKeySet();

    private final java.util.concurrent.ScheduledExecutorService timeoutScheduler =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Duels-MatchTimer");
            t.setDaemon(true);
            return t;
        });

    public MatchService(ServerAdapter server, ArenaService arenaService, KitService kitService,
                        RankingService rankingService, DuelEventBus eventBus, NotificationService notificationService, LongSupplier clock) {
        this.server = server;
        this.arenaService = arenaService;
        this.kitService = kitService;
        this.rankingService = rankingService;
        this.eventBus = eventBus;
        this.notificationService = notificationService;
        this.clock = clock;

        // Start the timeout checker (every 5 seconds)
        timeoutScheduler.scheduleAtFixedRate(this::checkMatchTimeouts, 5, 5, java.util.concurrent.TimeUnit.SECONDS);
    }

    public Result sendDuelRequest(UUID senderId, UUID targetId, String kitId) {
        long now = clock.getAsLong();

        if (senderId.equals(targetId)) {
            return Result.error("Tu ne peux pas te defier toi-meme.");
        }

        if (isInMatch(senderId)) {
            return Result.error("Tu es deja dans un match.");
        }

        if (isInMatch(targetId)) {
            return Result.error("Ce joueur est deja dans un match.");
        }

        Long lastRequest = lastRequestTime.get(senderId);
        if (lastRequest != null && now - lastRequest < REQUEST_COOLDOWN_MS) {
            return Result.error("Attends avant d'envoyer un autre defi.");
        }

        if (!kitService.kitExists(kitId)) {
            return Result.error("Kit inconnu: " + kitId);
        }

        cleanupExpiredRequests();

        DuelRequest request = DuelRequest.create(senderId, targetId, kitId, now);
        pendingRequests.put(targetId, request);
        lastRequestTime.put(senderId, now);

        return Result.success("Defi envoye!");
    }

    public Result acceptDuel(UUID targetId) {
        long now = clock.getAsLong();
        cleanupExpiredRequests();

        DuelRequest request = pendingRequests.remove(targetId);
        if (request == null) {
            return Result.error("Aucun defi en attente.");
        }

        if (request.isExpired(now)) {
            return Result.error("Le defi a expire.");
        }

        return startMatch(MatchType.DUEL_1V1, request.kitId(), List.of(request.senderId()), List.of(targetId));
    }

    public Result declineDuel(UUID targetId) {
        DuelRequest removed = pendingRequests.remove(targetId);
        if (removed == null) {
            return Result.error("Aucun defi en attente.");
        }
        return Result.success("Defi refuse.");
    }

    public Result startMatch(MatchType type, String kitId, List<UUID> team1, List<UUID> team2) {
        long now = clock.getAsLong();

        Optional<Arena> arenaOpt = arenaService.findAvailableArena(type.totalPlayers());
        if (arenaOpt.isEmpty()) {
            return Result.error("Aucune arene disponible.");
        }

        Arena arena = arenaOpt.get();
        if (!arenaService.reserveArena(arena.id())) {
            return Result.error("L'arene n'est plus disponible.");
        }

        Optional<KitDefinition> kitOpt = kitService.getKit(kitId);
        if (kitOpt.isEmpty()) {
            arenaService.releaseArena(arena.id());
            return Result.error("Kit inconnu: " + kitId);
        }

        String matchId = "match-" + matchIdCounter.getAndIncrement();
        Match match = new Match(matchId, arena.id(), kitId, type, team1, team2, now);

        activeMatches.put(matchId, match);
        for (UUID playerId : match.allPlayers()) {
            playerToMatch.put(playerId, matchId);
        }

        teleportAndEquipPlayers(match, arena, kitOpt.get());
        initiateMatchCountdown(match);

        return Result.success("Match demarre dans " + arena.displayName() + "!");
    }

    private void initiateMatchCountdown(Match match) {
        match.setStarting();

        // Countdown avec titres visuels
        showTitleToAll(match, "3", "Preparez-vous...", "#FFFF00", 0.1f, 0.8f, 0.1f);
        timeoutScheduler.schedule(() -> showTitleToAll(match, "2", "Preparez-vous...", "#FFA500", 0.1f, 0.8f, 0.1f), 1, java.util.concurrent.TimeUnit.SECONDS);
        timeoutScheduler.schedule(() -> showTitleToAll(match, "1", "Preparez-vous...", "#FF6600", 0.1f, 0.8f, 0.1f), 2, java.util.concurrent.TimeUnit.SECONDS);
        timeoutScheduler.schedule(() -> {
            match.start(clock.getAsLong());
            showTitleToAll(match, "COMBAT!", "", "#FF0000", 0.2f, 1.5f, 0.3f);

            // Fire event and notify
            eventBus.fire(new MatchStartedEvent(match));
            for (UUID playerId : match.allPlayers()) {
                 server.getPlayer(playerId).ifPresent(p -> {
                     String opponentName = match.getTeamOf(playerId) == 1 ? "Team 2" : "Team 1"; // Simplification
                     notificationService.sendMatchStart(p, opponentName);
                 });
            }
        }, 3, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void showTitleToAll(Match match, String title, String subtitle, String color, float fadeIn, float stay, float fadeOut) {
        for (UUID uuid : match.allPlayers()) {
            server.getPlayer(uuid).ifPresent(p -> server.showTitle(p, title, subtitle, color, fadeIn, stay, fadeOut));
        }
    }

    private void broadcast(Match match, String message) {
        match.allPlayers().forEach(uuid ->
            server.getPlayer(uuid).ifPresent(p -> p.sendMessage(message))
        );
    }

    private void teleportAndEquipPlayers(Match match, Arena arena, KitDefinition kit) {
        for (int i = 0; i < match.team1().size(); i++) {
            UUID playerId = match.team1().get(i);
            server.getPlayer(playerId).ifPresent(player -> {
                // Determine spawn point: use index if multiple spawns exist, or cycle
                com.kingc.hytale.duels.api.Location spawn;
                if (!arena.team1Spawns().isEmpty()) {
                    spawn = arena.team1Spawns().get(i % arena.team1Spawns().size());
                    player.teleport(spawn);
                }
                kitService.applyKit(player, kit);
            });
        }

        for (int i = 0; i < match.team2().size(); i++) {
            UUID playerId = match.team2().get(i);
            server.getPlayer(playerId).ifPresent(player -> {
                com.kingc.hytale.duels.api.Location spawn;
                if (!arena.team2Spawns().isEmpty()) {
                    spawn = arena.team2Spawns().get(i % arena.team2Spawns().size());
                    player.teleport(spawn);
                }
                kitService.applyKit(player, kit);
            });
        }
    }

    public void endMatch(String matchId, List<UUID> winners) {
        Match match = activeMatches.remove(matchId);
        if (match == null) {
            return;
        }

        long now = clock.getAsLong();
        match.end(winners, now);

        eventBus.fire(new MatchEndedEvent(match));

        for (UUID playerId : match.allPlayers()) {
            playerToMatch.remove(playerId);
        }

        arenaService.releaseArena(match.arenaId());

        // Mettre a jour le classement ELO
        if (!winners.isEmpty()) {
            updateRankings(match, winners);
        }

        // Return to lobby
        timeoutScheduler.schedule(() -> {
            for (java.util.UUID uuid : match.allPlayers()) {
                 server.getPlayer(uuid).ifPresent(p -> {
                     server.clearInventory(p);
                     server.teleportToLobby(p);
                     p.sendMessage("&7Returned to lobby.");
                 });
            }
        }, 3, java.util.concurrent.TimeUnit.SECONDS); // Delay lobby TP by 3 seconds to see results
    }

    private void updateRankings(Match match, List<UUID> winners) {
        List<UUID> losers = new ArrayList<>();
        for (UUID playerId : match.allPlayers()) {
            if (!winners.contains(playerId)) {
                losers.add(playerId);
            }
        }

        if (match.type() == MatchType.DUEL_1V1 && winners.size() == 1 && losers.size() == 1) {
            // Match 1v1
            UUID winnerId = winners.get(0);
            UUID loserId = losers.get(0);

            String winnerName = server.getPlayer(winnerId).map(PlayerRef::name).orElse("Unknown");
            String loserName = server.getPlayer(loserId).map(PlayerRef::name).orElse("Unknown");

            rankingService.recordMatch1v1(winnerId, winnerName, loserId, loserName);
        } else if (match.type() == MatchType.DUEL_2V2 && winners.size() == 2 && losers.size() == 2) {
            // Match 2v2
            List<String> winnerNames = new ArrayList<>();
            List<String> loserNames = new ArrayList<>();

            for (UUID id : winners) {
                winnerNames.add(server.getPlayer(id).map(PlayerRef::name).orElse("Unknown"));
            }
            for (UUID id : losers) {
                loserNames.add(server.getPlayer(id).map(PlayerRef::name).orElse("Unknown"));
            }

            rankingService.recordMatch2v2(winners, winnerNames, losers, loserNames);
        }
    }

    public void handlePlayerDeath(UUID playerId) {
        String matchId = playerToMatch.get(playerId);
        if (matchId == null) {
            return;
        }

        Match match = activeMatches.get(matchId);
        if (match == null || match.state() != Match.MatchState.PLAYING) {
            return;
        }

        int team = match.getTeamOf(playerId);
        List<UUID> winners = (team == 1) ? match.team2() : match.team1();
        List<UUID> losers = (team == 1) ? match.team1() : match.team2();
        endMatch(matchId, winners);

        // Titres visuels pour victoire/défaite
        for (UUID winnerId : winners) {
            server.getPlayer(winnerId).ifPresent(p -> {
                // TODO: Calculate actual ELO gain to pass here
                notificationService.sendVictory(p, 0);
            });
        }
        for (UUID loserId : losers) {
            server.getPlayer(loserId).ifPresent(p -> {
                notificationService.sendDefeat(p, 0);
            });
        }
    }

    public boolean isInMatch(UUID playerId) {
        return playerToMatch.containsKey(playerId);
    }

    public Optional<Match> getPlayerMatch(UUID playerId) {
        String matchId = playerToMatch.get(playerId);
        if (matchId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeMatches.get(matchId));
    }

    public Optional<DuelRequest> getPendingRequest(UUID targetId) {
        cleanupExpiredRequests();
        return Optional.ofNullable(pendingRequests.get(targetId));
    }

    public java.util.Collection<Match> getActiveMatches() {
        return activeMatches.values();
    }

    private void cleanupExpiredRequests() {
        long now = clock.getAsLong();
        pendingRequests.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private void checkMatchTimeouts() {
        long now = clock.getAsLong();

        for (Match match : activeMatches.values()) {
            if (match.state() != Match.MatchState.PLAYING) continue;

            long elapsed = now - match.startedAt();

            // 1-minute warning
            if (elapsed >= WARNING_TIME_MS && !warnedMatches.contains(match.matchId())) {
                warnedMatches.add(match.matchId());
                for (UUID playerId : match.allPlayers()) {
                    server.getPlayer(playerId).ifPresent(p ->
                        p.sendMessage("[Duels] ⚠ 1 minute restante!")
                    );
                }
            }

            // Timeout - end as draw
            if (elapsed >= MATCH_TIMEOUT_MS) {
                endMatchAsDraw(match.matchId());
            }
        }
    }

    private void endMatchAsDraw(String matchId) {
        Match match = activeMatches.remove(matchId);
        if (match == null) return;

        warnedMatches.remove(matchId);

        for (UUID playerId : match.allPlayers()) {
            playerToMatch.remove(playerId);
            server.getPlayer(playerId).ifPresent(p -> {
                server.showTitle(p, "MATCH NUL", "Temps ecoule!", "#AAAAAA", 0.3f, 3.5f, 0.5f);
            });
        }

        arenaService.releaseArena(match.arenaId());
        // No ELO change for draw

        // Return to lobby after delay
        timeoutScheduler.schedule(() -> {
            for (UUID uuid : match.allPlayers()) {
                server.getPlayer(uuid).ifPresent(p -> {
                    server.clearInventory(p);
                    server.teleportToLobby(p);
                });
            }
        }, 3, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void shutdown() {
        timeoutScheduler.shutdownNow();
    }

    public record Result(boolean success, String message) {
        public static Result success(String message) {
            return new Result(true, message);
        }

        public static Result error(String message) {
            return new Result(false, message);
        }
    }
}
