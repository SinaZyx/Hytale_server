package com.kingc.hytale.duels.integration;

import com.kingc.hytale.duels.api.PlayerRef;
import com.kingc.hytale.duels.ranking.PlayerStats;
import com.kingc.hytale.duels.ranking.RankingService;

public class DuelsPlaceholder {
    private final RankingService rankingService;

    public DuelsPlaceholder(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    public String replace(String input, PlayerRef player) {
        if (input == null || player == null) return input;

        java.util.Optional<PlayerStats> statsOpt = rankingService.getStats(player.id());
        if (statsOpt.isEmpty()) return input;

        PlayerStats stats = statsOpt.get();
        return input
            .replace("{duels_elo}", String.valueOf(stats.elo()))
            .replace("{duels_rank}", stats.getRank().displayName())
            .replace("{duels_wins}", String.valueOf(stats.wins()))
            .replace("{duels_losses}", String.valueOf(stats.losses()))
            .replace("{duels_winrate}", String.format("%.1f", stats.winRate()));
    }
}
