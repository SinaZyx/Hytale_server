package com.kingc.hytale.duels.ranking;

import com.kingc.hytale.duels.api.ServerAdapter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

public final class RankingService {
    private final RankingRepository repository;
    private final ServerAdapter server;
    private final LongSupplier clock;

    public RankingService(RankingRepository repository, ServerAdapter server, LongSupplier clock) {
        this.repository = repository;
        this.server = server;
        this.clock = clock;
    }

    /**
     * Enregistre le resultat d'un match 1v1
     */
    public MatchResult recordMatch1v1(UUID winnerId, String winnerName, UUID loserId, String loserName) {
        long now = clock.getAsLong();

        PlayerStats winner = repository.getOrCreate(winnerId, winnerName);
        PlayerStats loser = repository.getOrCreate(loserId, loserName);

        // Mettre a jour les noms au cas ou ils auraient change
        winner.setPlayerName(winnerName);
        loser.setPlayerName(loserName);

        int winnerOldElo = winner.elo();
        int loserOldElo = loser.elo();
        Rank winnerOldRank = winner.getRank();
        Rank loserOldRank = loser.getRank();

        // Calculer le changement d'ELO
        EloCalculator.EloChange change = EloCalculator.calculate(
            winnerOldElo,
            loserOldElo,
            winner.totalMatches(),
            winner.winStreak()
        );

        // Appliquer les changements
        winner.recordWin(now);
        winner.addElo(change.winnerChange());

        loser.recordLoss(now);
        loser.addElo(change.loserChange());

        // Sauvegarder
        repository.update(winner);
        repository.update(loser);

        // Notifier les joueurs
        notifyPlayer(winnerId, winner, winnerOldElo, winnerOldRank, true);
        notifyPlayer(loserId, loser, loserOldElo, loserOldRank, false);

        return new MatchResult(
            winnerId, loserId,
            change.winnerChange(), change.loserChange(),
            winner.elo(), loser.elo(),
            winner.getRank() != winnerOldRank,
            loser.getRank() != loserOldRank
        );
    }

    /**
     * Enregistre le resultat d'un match 2v2
     */
    public void recordMatch2v2(List<UUID> winnerIds, List<String> winnerNames,
                               List<UUID> loserIds, List<String> loserNames) {
        if (winnerIds.size() != 2 || loserIds.size() != 2) {
            return;
        }

        long now = clock.getAsLong();

        PlayerStats winner1 = repository.getOrCreate(winnerIds.get(0), winnerNames.get(0));
        PlayerStats winner2 = repository.getOrCreate(winnerIds.get(1), winnerNames.get(1));
        PlayerStats loser1 = repository.getOrCreate(loserIds.get(0), loserNames.get(0));
        PlayerStats loser2 = repository.getOrCreate(loserIds.get(1), loserNames.get(1));

        // Calculer le changement d'ELO base sur la moyenne des equipes
        EloCalculator.EloChange change = EloCalculator.calculate2v2(
            winner1.elo(), winner2.elo(),
            loser1.elo(), loser2.elo(),
            true
        );

        // Appliquer aux gagnants
        for (PlayerStats winner : List.of(winner1, winner2)) {
            int oldElo = winner.elo();
            Rank oldRank = winner.getRank();
            winner.recordWin(now);
            winner.addElo(change.winnerChange());
            repository.update(winner);
            notifyPlayer(winner.playerId(), winner, oldElo, oldRank, true);
        }

        // Appliquer aux perdants
        for (PlayerStats loser : List.of(loser1, loser2)) {
            int oldElo = loser.elo();
            Rank oldRank = loser.getRank();
            loser.recordLoss(now);
            loser.addElo(change.loserChange());
            repository.update(loser);
            notifyPlayer(loser.playerId(), loser, oldElo, oldRank, false);
        }
    }

    private void notifyPlayer(UUID playerId, PlayerStats stats, int oldElo, Rank oldRank, boolean won) {
        server.getPlayer(playerId).ifPresent(player -> {
            int eloChange = stats.elo() - oldElo;
            String sign = eloChange >= 0 ? "+" : "";
            String eloMsg = "[Duels] " + sign + eloChange + " ELO (" + stats.elo() + ")";
            player.sendMessage(eloMsg);

            // Notification de changement de rang
            Rank newRank = stats.getRank();
            if (newRank != oldRank) {
                if (newRank.ordinal() > oldRank.ordinal()) {
                    player.sendMessage("[Duels] Promotion! Tu es maintenant " + newRank.displayName() + "!");
                } else {
                    player.sendMessage("[Duels] Relegation: " + newRank.displayName());
                }
            }
        });
    }

    public Optional<PlayerStats> getStats(UUID playerId) {
        return repository.get(playerId);
    }

    public PlayerStats getOrCreateStats(UUID playerId, String playerName) {
        return repository.getOrCreate(playerId, playerName);
    }

    public List<PlayerStats> getLeaderboard(int limit) {
        return repository.getTopPlayers(limit);
    }

    public List<PlayerStats> getLeaderboardByWins(int limit) {
        return repository.getTopPlayersByWins(limit);
    }

    public List<PlayerStats> getLeaderboardByWinRate(int limit) {
        return repository.getTopPlayersByWinRate(limit, 10); // Minimum 10 matchs
    }

    public int getPlayerRank(UUID playerId) {
        return repository.getRank(playerId);
    }

    public int getTotalPlayers() {
        return repository.getTotalPlayers();
    }

    public void save() throws IOException {
        repository.save();
    }

    public record MatchResult(
        UUID winnerId,
        UUID loserId,
        int winnerEloChange,
        int loserEloChange,
        int winnerNewElo,
        int loserNewElo,
        boolean winnerRankChanged,
        boolean loserRankChanged
    ) {}
}
