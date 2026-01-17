package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kingc.hytale.duels.ranking.PlayerStats;
import com.kingc.hytale.duels.ranking.Rank;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class RankingMenuPage extends InteractiveCustomUIPage<RankingMenuPage.RankingEventData> {
    private static final String PAGE_UI = "Pages/RankingMenu.ui";
    private static final int LEADERBOARD_SIZE = 20;

    private final HytaleDuelsPlugin plugin;
    private String activeTab = "leaderboard";
    private String sortBy = "elo"; // elo, wins, winrate

    public RankingMenuPage(HytaleDuelsPlugin plugin, PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, RankingEventData.CODEC);
        this.plugin = plugin;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events, Store<EntityStore> store) {
        commands.append(PAGE_UI);
        updatePlayerStats(commands);
        applyTabState(commands);
        buildContent(commands);
        bindActions(events);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, RankingEventData data) {
        if (data == null) {
            return;
        }
        String action = normalize(data.getAction());
        if (action == null) {
            return;
        }
        action = action.toLowerCase(Locale.ROOT);

        switch (action) {
            case "tab_leaderboard" -> activeTab = "leaderboard";
            case "tab_mystats" -> activeTab = "mystats";
            case "tab_ranks" -> activeTab = "ranks";
            case "sort_elo" -> sortBy = "elo";
            case "sort_wins" -> sortBy = "wins";
            case "sort_winrate" -> sortBy = "winrate";
        }
        refresh();
    }

    private void refresh() {
        UICommandBuilder commands = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        updatePlayerStats(commands);
        applyTabState(commands);
        buildContent(commands);
        bindActions(events);
        sendUpdate(commands, events, false);
    }

    private void updatePlayerStats(UICommandBuilder commands) {
        UUID playerId = playerRef.getUuid();
        if (playerId == null) {
            return;
        }

        String playerName = playerRef.getUsername();
        if (playerName == null) {
            playerName = "Unknown";
        }
        PlayerStats stats = plugin.core().rankingService().getOrCreateStats(playerId, playerName);
        Rank rank = stats.getRank();
        int position = plugin.core().rankingService().getPlayerRank(playerId);
        int totalPlayers = plugin.core().rankingService().getTotalPlayers();

        commands.set("#PlayerRank.TextSpans", Message.raw(rank.displayName()));
        commands.set("#PlayerElo.TextSpans", Message.raw(stats.elo() + " ELO"));
        commands.set("#PlayerPosition.TextSpans", Message.raw("#" + position + " / " + totalPlayers));
        commands.set("#PlayerWinLoss.TextSpans", Message.raw(stats.wins() + "W / " + stats.losses() + "L"));
    }

    private void applyTabState(UICommandBuilder commands) {
        boolean leaderboard = "leaderboard".equals(activeTab);
        boolean mystats = "mystats".equals(activeTab);
        boolean ranks = "ranks".equals(activeTab);

        commands.set("#LeaderboardView.Visible", leaderboard);
        commands.set("#MyStatsView.Visible", mystats);
        commands.set("#RanksView.Visible", ranks);

        commands.set("#NavLeaderboardIndicator.Visible", leaderboard);
        commands.set("#NavMyStatsIndicator.Visible", mystats);
        commands.set("#NavRanksIndicator.Visible", ranks);

        // Sort buttons active state
        commands.set("#SortEloActive.Visible", "elo".equals(sortBy));
        commands.set("#SortWinsActive.Visible", "wins".equals(sortBy));
        commands.set("#SortWinrateActive.Visible", "winrate".equals(sortBy));
    }

    private void buildContent(UICommandBuilder commands) {
        switch (activeTab) {
            case "leaderboard" -> buildLeaderboard(commands);
            case "mystats" -> buildMyStats(commands);
            case "ranks" -> buildRanksInfo(commands);
        }
    }

    private void buildLeaderboard(UICommandBuilder commands) {
        commands.clear("#LeaderboardList");

        List<PlayerStats> leaderboard = switch (sortBy) {
            case "wins" -> plugin.core().rankingService().getLeaderboardByWins(LEADERBOARD_SIZE);
            case "winrate" -> plugin.core().rankingService().getLeaderboardByWinRate(LEADERBOARD_SIZE);
            default -> plugin.core().rankingService().getLeaderboard(LEADERBOARD_SIZE);
        };

        if (leaderboard.isEmpty()) {
            commands.appendInline("#LeaderboardList",
                "Label { Text: \"Aucun joueur classe\"; Style: (Alignment: Center, FontSize: 12, TextColor: #9aa7b0); }");
            return;
        }

        UUID myId = playerRef.getUuid();
        int position = 1;

        for (PlayerStats stats : leaderboard) {
            boolean isMe = stats.playerId().equals(myId);
            Rank rank = stats.getRank();
            String bgColor = isMe ? "#3a3a5a" : "#2a2a2a";
            String textColor = isMe ? "#4fc3f7" : "#d7e0e8";

            String positionStr = getPositionEmoji(position) + " #" + position;
            String statValue = switch (sortBy) {
                case "wins" -> stats.wins() + " wins";
                case "winrate" -> String.format("%.1f%%", stats.winRate());
                default -> stats.elo() + " ELO";
            };

            String line = positionStr + " | " + escapeUiText(stats.playerName()) + " | " + rank.displayName() + " | " + statValue;
            String entry = "Label { Text: \"" + line + "\"; Style: (FontSize: 12, TextColor: " + textColor + "); Anchor: (Bottom: 6); }";

            commands.appendInline("#LeaderboardList", entry);
            position++;
        }
    }

    private void buildMyStats(UICommandBuilder commands) {
        UUID playerId = playerRef.getUuid();
        if (playerId == null) {
            return;
        }

        String playerName = playerRef.getUsername();
        if (playerName == null) {
            playerName = "Unknown";
        }
        PlayerStats stats = plugin.core().rankingService().getOrCreateStats(playerId, playerName);
        Rank rank = stats.getRank();
        Rank nextRank = rank.next();

        // Stats detaillees
        commands.set("#StatElo.TextSpans", Message.raw(String.valueOf(stats.elo())));
        commands.set("#StatWins.TextSpans", Message.raw(String.valueOf(stats.wins())));
        commands.set("#StatLosses.TextSpans", Message.raw(String.valueOf(stats.losses())));
        commands.set("#StatWinRate.TextSpans", Message.raw(String.format("%.1f%%", stats.winRate())));
        commands.set("#StatWinStreak.TextSpans", Message.raw(String.valueOf(stats.winStreak())));
        commands.set("#StatBestStreak.TextSpans", Message.raw(String.valueOf(stats.bestWinStreak())));
        commands.set("#StatTotalMatches.TextSpans", Message.raw(String.valueOf(stats.totalMatches())));

        // Progression vers le rang suivant (barre de 400px max)
        if (rank != Rank.CHAMPION) {
            int eloNeeded = nextRank.minElo() - stats.elo();
            int progress = (int) (((double)(stats.elo() - rank.minElo()) / (nextRank.minElo() - rank.minElo())) * 400);
            commands.set("#NextRankName.TextSpans", Message.raw(nextRank.displayName()));
            commands.set("#NextRankElo.TextSpans", Message.raw("+" + eloNeeded + " ELO"));
            commands.set("#ProgressBar.Anchor", "(Height: 8, Width: " + Math.max(10, progress) + ")");
        } else {
            commands.set("#NextRankName.TextSpans", Message.raw("Rang maximum!"));
            commands.set("#NextRankElo.TextSpans", Message.raw(""));
            commands.set("#ProgressBar.Anchor", "(Height: 8, Width: 400)");
        }
    }

    private void buildRanksInfo(UICommandBuilder commands) {
        commands.clear("#RanksList");

        for (Rank rank : Rank.values()) {
            int playersInRank = plugin.core().rankingService().getTotalPlayers() > 0 ?
                getPlayersInRank(rank) : 0;

            String eloRange = rank == Rank.CHAMPION ?
                rank.minElo() + "+" :
                rank.minElo() + " - " + rank.maxElo();

            String line = rank.displayName() + " | " + eloRange + " ELO | " + playersInRank + " joueurs";
            String entry = "Label { Text: \"" + line + "\"; Style: (FontSize: 12, TextColor: " + rank.color() + "); Anchor: (Bottom: 6); }";

            commands.appendInline("#RanksList", entry);
        }
    }

    private int getPlayersInRank(Rank rank) {
        // Compter les joueurs dans ce rang
        return (int) plugin.core().rankingService().getLeaderboard(1000).stream()
            .filter(s -> s.getRank() == rank)
            .count();
    }

    private void bindActions(UIEventBuilder events) {
        // Navigation
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavLeaderboardButton",
            EventData.of(RankingEventData.KEY_ACTION, "tab_leaderboard"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavMyStatsButton",
            EventData.of(RankingEventData.KEY_ACTION, "tab_mystats"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavRanksButton",
            EventData.of(RankingEventData.KEY_ACTION, "tab_ranks"));

        // Sort buttons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SortEloButton",
            EventData.of(RankingEventData.KEY_ACTION, "sort_elo"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SortWinsButton",
            EventData.of(RankingEventData.KEY_ACTION, "sort_wins"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SortWinrateButton",
            EventData.of(RankingEventData.KEY_ACTION, "sort_winrate"));
    }

    private String getPositionEmoji(int position) {
        return switch (position) {
            case 1 -> ""; // Or use unicode medal if supported
            case 2 -> "";
            case 3 -> "";
            default -> "";
        };
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String escapeUiText(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static final class RankingEventData {
        static final String KEY_ACTION = "Action";

        static final BuilderCodec<RankingEventData> CODEC = BuilderCodec.builder(RankingEventData.class, RankingEventData::new)
            .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), RankingEventData::setAction, RankingEventData::getAction).add()
            .build();

        private String action;

        public RankingEventData() {}

        public String getAction() { return action; }
        private void setAction(String action) { this.action = action; }
    }
}
