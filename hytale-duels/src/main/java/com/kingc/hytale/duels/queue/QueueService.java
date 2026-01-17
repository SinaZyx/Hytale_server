package com.kingc.hytale.duels.queue;

import com.kingc.hytale.duels.api.ServerAdapter;
import com.kingc.hytale.duels.match.MatchService;
import com.kingc.hytale.duels.match.MatchType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.LongSupplier;

public final class QueueService {
    private final ServerAdapter server;
    private final MatchService matchService;
    private final LongSupplier clock;

    private final Map<MatchType, ConcurrentLinkedQueue<QueueEntry>> queues = new ConcurrentHashMap<>();
    private final Map<UUID, MatchType> playerQueue = new ConcurrentHashMap<>();

    public QueueService(ServerAdapter server, MatchService matchService, LongSupplier clock) {
        this.server = server;
        this.matchService = matchService;
        this.clock = clock;

        for (MatchType type : MatchType.values()) {
            queues.put(type, new ConcurrentLinkedQueue<>());
        }
    }

    public Result joinQueue(UUID playerId, MatchType type, String kitId) {
        if (matchService.isInMatch(playerId)) {
            return Result.error("Tu es deja dans un match.");
        }

        if (playerQueue.containsKey(playerId)) {
            return Result.error("Tu es deja dans une file d'attente. Utilise /queue leave pour quitter.");
        }

        long now = clock.getAsLong();
        QueueEntry entry = new QueueEntry(playerId, type, kitId, now);

        queues.get(type).add(entry);
        playerQueue.put(playerId, type);

        tryMatchPlayers(type);

        if (playerQueue.containsKey(playerId)) {
            int position = getQueuePosition(playerId, type);
            return Result.success("File " + type.name() + " rejointe. Position: " + position);
        } else {
            return Result.success("Match trouve!");
        }
    }

    public Result leaveQueue(UUID playerId) {
        MatchType type = playerQueue.remove(playerId);
        if (type == null) {
            return Result.error("Tu n'es pas dans une file d'attente.");
        }

        ConcurrentLinkedQueue<QueueEntry> queue = queues.get(type);
        queue.removeIf(entry -> entry.playerId().equals(playerId));

        return Result.success("File d'attente quittee.");
    }

    public boolean isInQueue(UUID playerId) {
        return playerQueue.containsKey(playerId);
    }

    public Optional<MatchType> getPlayerQueueType(UUID playerId) {
        return Optional.ofNullable(playerQueue.get(playerId));
    }

    public int getQueueSize(MatchType type) {
        return queues.get(type).size();
    }

    private int getQueuePosition(UUID playerId, MatchType type) {
        int position = 1;
        for (QueueEntry entry : queues.get(type)) {
            if (entry.playerId().equals(playerId)) {
                return position;
            }
            position++;
        }
        return -1;
    }

    private void tryMatchPlayers(MatchType type) {
        ConcurrentLinkedQueue<QueueEntry> queue = queues.get(type);
        int required = type.totalPlayers();

        while (queue.size() >= required) {
            List<QueueEntry> matched = new ArrayList<>();
            Iterator<QueueEntry> iter = queue.iterator();

            while (matched.size() < required && iter.hasNext()) {
                QueueEntry entry = iter.next();
                if (server.getPlayer(entry.playerId()).map(p -> p.isOnline()).orElse(false)) {
                    matched.add(entry);
                } else {
                    iter.remove();
                    playerQueue.remove(entry.playerId());
                }
            }

            if (matched.size() < required) {
                break;
            }

            for (QueueEntry entry : matched) {
                queue.remove(entry);
                playerQueue.remove(entry.playerId());
            }

            String kitId = matched.get(0).kitId();
            int teamSize = type.teamSize();

            List<UUID> team1 = matched.subList(0, teamSize).stream()
                .map(QueueEntry::playerId)
                .toList();
            List<UUID> team2 = matched.subList(teamSize, required).stream()
                .map(QueueEntry::playerId)
                .toList();

            MatchService.Result result = matchService.startMatch(type, kitId, team1, team2);

            if (!result.success()) {
                for (QueueEntry entry : matched) {
                    queue.add(entry);
                    playerQueue.put(entry.playerId(), type);
                    server.getPlayer(entry.playerId())
                        .ifPresent(p -> p.sendMessage("[Duels] " + result.message()));
                }
            }
        }
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
