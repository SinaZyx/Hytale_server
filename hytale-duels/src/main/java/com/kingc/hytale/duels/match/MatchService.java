package com.kingc.hytale.duels.match;

import com.kingc.hytale.duels.api.PlayerRef;
import com.kingc.hytale.duels.api.ServerAdapter;
import com.kingc.hytale.duels.arena.Arena;
import com.kingc.hytale.duels.arena.ArenaService;
import com.kingc.hytale.duels.kit.KitDefinition;
import com.kingc.hytale.duels.kit.KitService;
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

    private final ServerAdapter server;
    private final ArenaService arenaService;
    private final KitService kitService;
    private final RankingService rankingService;
    private final LongSupplier clock;

    private final Map<UUID, DuelRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastRequestTime = new ConcurrentHashMap<>();
    private final Map<String, Match> activeMatches = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerToMatch = new ConcurrentHashMap<>();
    private final AtomicInteger matchIdCounter = new AtomicInteger(1);

    public MatchService(ServerAdapter server, ArenaService arenaService, KitService kitService,
                        RankingService rankingService, LongSupplier clock) {
        this.server = server;
        this.arenaService = arenaService;
        this.kitService = kitService;
        this.rankingService = rankingService;
        this.clock = clock;
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
        match.start(now);

        return Result.success("Match demarre dans " + arena.displayName() + "!");
    }

    private void teleportAndEquipPlayers(Match match, Arena arena, KitDefinition kit) {
        for (int i = 0; i < match.team1().size(); i++) {
            UUID playerId = match.team1().get(i);
            server.getPlayer(playerId).ifPresent(player -> {
                if (!arena.team1Spawns().isEmpty()) {
                    player.teleport(arena.team1Spawns().get(0));
                }
                kitService.applyKit(player, kit);
            });
        }

        for (int i = 0; i < match.team2().size(); i++) {
            UUID playerId = match.team2().get(i);
            server.getPlayer(playerId).ifPresent(player -> {
                if (!arena.team2Spawns().isEmpty()) {
                    player.teleport(arena.team2Spawns().get(0));
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

        for (UUID playerId : match.allPlayers()) {
            playerToMatch.remove(playerId);
        }

        arenaService.releaseArena(match.arenaId());

        // Mettre a jour le classement ELO
        if (!winners.isEmpty()) {
            updateRankings(match, winners);
        }
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
        if (match == null || match.state() != MatchState.RUNNING) {
            return;
        }

        int team = match.getTeamOf(playerId);
        List<UUID> winners = (team == 1) ? match.team2() : match.team1();
        endMatch(matchId, winners);

        for (UUID winnerId : winners) {
            server.getPlayer(winnerId).ifPresent(p -> p.sendMessage("[Duels] Victoire!"));
        }
        server.getPlayer(playerId).ifPresent(p -> p.sendMessage("[Duels] Defaite."));
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

    public record Result(boolean success, String message) {
        public static Result success(String message) {
            return new Result(true, message);
        }

        public static Result error(String message) {
            return new Result(false, message);
        }
    }
}
