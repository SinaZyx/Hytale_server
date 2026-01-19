package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.kingc.hytale.duels.ranking.PlayerStats;

import javax.annotation.Nonnull;
import java.util.List;

public final class TopCommand extends CommandBase {
    private final HytaleDuelsPlugin plugin;

    public TopCommand(HytaleDuelsPlugin plugin) {
        super("top", "View leaderboard");
        this.plugin = plugin;
        this.addAliases("leaderboard");
        this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        List<PlayerStats> topPlayers = plugin.core().rankingService().getLeaderboard(10);
        
        ctx.sendMessage(Message.raw(""));
        ctx.sendMessage(Message.raw("&6&l═══ 🏆 CLASSEMENT DUELS ═══"));
        ctx.sendMessage(Message.raw(""));
        
        if (topPlayers.isEmpty()) {
            ctx.sendMessage(Message.raw("&7Aucun joueur classé pour le moment."));
        } else {
            int rank = 1;
            for (PlayerStats stats : topPlayers) {
                String medal = getMedal(rank);
                String winRate = stats.totalMatches() > 0
                    ? String.format("%.0f%%", (stats.wins() * 100.0 / stats.totalMatches()))
                    : "0%";
                    
                ctx.sendMessage(Message.raw(String.format(
                    "&e%s #%d &f%s &7- &6%d ELO &7(%dW/%dL, %s)",
                    medal, rank, stats.playerName(), stats.elo(), 
                    stats.wins(), stats.losses(), winRate
                )));
                rank++;
            }
        }
        
        ctx.sendMessage(Message.raw(""));
        ctx.sendMessage(Message.raw("&6&l═══════════════════════════"));
    }
    
    private String getMedal(int rank) {
        return switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "  ";
        };
    }
}
