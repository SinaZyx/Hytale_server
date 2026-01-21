package com.kingc.hytale.duels.notifications;

import com.kingc.hytale.duels.api.PlayerRef;
import com.kingc.hytale.duels.api.ServerAdapter;
import com.kingc.hytale.duels.ranking.PlayerStats;

import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private final ServerAdapter server;
    private final List<NotificationEntry> history = new ArrayList<>();

    public NotificationService(ServerAdapter server) {
        this.server = server;
    }

    public void sendMatchStart(PlayerRef player, String opponentName) {
        server.showTitle(player, "DUEL", "vs " + opponentName, "#FF0000", 0.5f, 2.0f, 0.5f);
        // Assuming NotificationUtil is wrapped in ServerAdapter or we use native if available
        // But for now titles are good.
    }

    public void sendVictory(PlayerRef player, int eloGain) {
        server.showTitle(player, "VICTORY", "+" + eloGain + " ELO", "#00FF00", 0.5f, 3.0f, 1.0f);
        // TODO: Toast notification if API available
    }

    public void sendDefeat(PlayerRef player, int eloLoss) {
        server.showTitle(player, "DEFEAT", "-" + eloLoss + " ELO", "#FF0000", 0.5f, 3.0f, 1.0f);
    }

    public void sendRankUp(PlayerRef player, String rankName) {
        server.showTitle(player, "RANK UP!", rankName, "#FFFF00", 0.5f, 4.0f, 1.0f);
    }

    public void addHistory(NotificationType type, String message) {
        history.add(new NotificationEntry(type, message, System.currentTimeMillis()));
    }

    public List<NotificationEntry> getHistory() {
        return new ArrayList<>(history);
    }
}
