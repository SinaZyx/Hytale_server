package com.kingc.hytale.duels.command;

import com.kingc.hytale.duels.api.CommandSource;
import com.kingc.hytale.duels.api.PlayerRef;
import com.kingc.hytale.duels.api.ServerAdapter;
import com.kingc.hytale.duels.kit.KitDefinition;
import com.kingc.hytale.duels.kit.KitService;
import com.kingc.hytale.duels.match.MatchService;
import com.kingc.hytale.duels.match.MatchType;
import com.kingc.hytale.duels.queue.QueueService;
import com.kingc.hytale.duels.ranking.PlayerStats;
import com.kingc.hytale.duels.ranking.RankingService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CommandDispatcher {
    private static final String PREFIX = "[Duels] ";

    private final MatchService matchService;
    private final QueueService queueService;
    private final KitService kitService;
    private final RankingService rankingService;
    private final ServerAdapter server;

    public CommandDispatcher(MatchService matchService, QueueService queueService, KitService kitService,
                             RankingService rankingService, ServerAdapter server) {
        this.matchService = matchService;
        this.queueService = queueService;
        this.kitService = kitService;
        this.rankingService = rankingService;
        this.server = server;
    }

    public boolean handle(CommandSource source, String commandLine) {
        String[] parts = commandLine.trim().split("\\s+", 2);
        if (parts.length == 0) {
            return false;
        }

        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        return switch (command) {
            case "duel" -> handleDuel(source, args);
            case "queue" -> handleQueue(source, args);
            case "kit" -> handleKit(source, args);
            case "stats" -> handleStats(source, args);
            case "top", "leaderboard" -> handleLeaderboard(source, args);
            default -> false;
        };
    }

    private boolean handleDuel(CommandSource source, String args) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length == 0 || parts[0].isEmpty()) {
            source.sendMessage(PREFIX + "Usage: /duel <joueur> [kit] | /duel accept | /duel decline");
            return true;
        }

        String subCommand = parts[0].toLowerCase();
        Optional<UUID> playerIdOpt = source.playerId();
        if (playerIdOpt.isEmpty()) {
            source.sendMessage(PREFIX + "Commande joueur uniquement.");
            return true;
        }

        UUID playerId = playerIdOpt.get();

        return switch (subCommand) {
            case "accept" -> {
                MatchService.Result result = matchService.acceptDuel(playerId);
                source.sendMessage(PREFIX + result.message());
                yield true;
            }
            case "decline" -> {
                MatchService.Result result = matchService.declineDuel(playerId);
                source.sendMessage(PREFIX + result.message());
                yield true;
            }
            case "help" -> {
                // Signal to open help menu - handled by HytaleDuelsPlugin wrapper
                source.sendMessage(PREFIX + "OPEN_HELP_MENU");
                yield true;
            }
            default -> {
                String targetName = subCommand;
                String kitId = parts.length > 1 ? parts[1] : "tank";

                Optional<PlayerRef> targetOpt = server.getPlayerByName(targetName);
                if (targetOpt.isEmpty()) {
                    source.sendMessage(PREFIX + "Joueur introuvable: " + targetName);
                    yield true;
                }

                MatchService.Result result = matchService.sendDuelRequest(playerId, targetOpt.get().id(), kitId);
                source.sendMessage(PREFIX + result.message());

                if (result.success()) {
                    targetOpt.get().sendMessage(PREFIX + source.player().map(PlayerRef::name).orElse("Quelqu'un")
                        + " te defie en duel! /duel accept ou /duel decline");
                }
                yield true;
            }
        };
    }

    private boolean handleQueue(CommandSource source, String args) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length == 0 || parts[0].isEmpty()) {
            source.sendMessage(PREFIX + "Usage: /queue 1v1 [kit] | /queue 2v2 [kit] | /queue leave | /queue status");
            return true;
        }

        Optional<UUID> playerIdOpt = source.playerId();
        if (playerIdOpt.isEmpty()) {
            source.sendMessage(PREFIX + "Commande joueur uniquement.");
            return true;
        }

        UUID playerId = playerIdOpt.get();
        String subCommand = parts[0].toLowerCase();

        return switch (subCommand) {
            case "leave" -> {
                QueueService.Result result = queueService.leaveQueue(playerId);
                source.sendMessage(PREFIX + result.message());
                yield true;
            }
            case "status" -> {
                if (queueService.isInQueue(playerId)) {
                    MatchType type = queueService.getPlayerQueueType(playerId).orElse(MatchType.DUEL_1V1);
                    source.sendMessage(PREFIX + "En file " + type.name() + " (" + queueService.getQueueSize(type) + " joueurs)");
                } else if (matchService.isInMatch(playerId)) {
                    source.sendMessage(PREFIX + "En match.");
                } else {
                    source.sendMessage(PREFIX + "Pas en file.");
                }
                yield true;
            }
            case "1v1" -> {
                String kitId = parts.length > 1 ? parts[1] : "tank";
                QueueService.Result result = queueService.joinQueue(playerId, MatchType.DUEL_1V1, kitId);
                source.sendMessage(PREFIX + result.message());
                yield true;
            }
            case "2v2" -> {
                String kitId = parts.length > 1 ? parts[1] : "tank";
                QueueService.Result result = queueService.joinQueue(playerId, MatchType.DUEL_2V2, kitId);
                source.sendMessage(PREFIX + result.message());
                yield true;
            }
            default -> {
                source.sendMessage(PREFIX + "Usage: /queue 1v1 [kit] | /queue 2v2 [kit] | /queue leave");
                yield true;
            }
        };
    }

    private boolean handleKit(CommandSource source, String args) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length == 0 || parts[0].isEmpty()) {
            source.sendMessage(PREFIX + "Usage: /kit list | /kit info <nom> | /kit preview <nom> | /kit save <nom> | /kit delete <nom>");
            return true;
        }

        String subCommand = parts[0].toLowerCase();

        return switch (subCommand) {
            case "list" -> {
                source.sendMessage(PREFIX + "Kits disponibles:");
                for (KitDefinition kit : kitService.getAllKits()) {
                    source.sendMessage("  - " + kit.displayName() + " (" + kit.id() + ")");
                }
                yield true;
            }
            case "info" -> {
                if (parts.length < 2) {
                    source.sendMessage(PREFIX + "Usage: /kit info <nom>");
                    yield true;
                }
                String kitId = parts[1];
                Optional<KitDefinition> kitOpt = kitService.getKit(kitId);
                if (kitOpt.isEmpty()) {
                    source.sendMessage(PREFIX + "Kit inconnu: " + kitId);
                    yield true;
                }
                KitDefinition kit = kitOpt.get();
                source.sendMessage(PREFIX + kit.displayName());
                if (kit.items() != null) {
                    source.sendMessage("  Items: " + kit.items().size());
                }
                if (kit.effects() != null && !kit.effects().isEmpty()) {
                    source.sendMessage("  Effets: " + kit.effects().keySet());
                }
                yield true;
            }
            case "preview" -> {
                if (parts.length < 2) {
                    source.sendMessage(PREFIX + "Usage: /kit preview <nom>");
                    yield true;
                }
                Optional<UUID> playerIdOpt = source.playerId();
                if (playerIdOpt.isEmpty()) {
                    source.sendMessage(PREFIX + "Commande joueur uniquement.");
                    yield true;
                }
                String kitId = parts[1];
                Optional<KitDefinition> kitOpt = kitService.getKit(kitId);
                if (kitOpt.isEmpty()) {
                    source.sendMessage(PREFIX + "Kit inconnu: " + kitId);
                    yield true;
                }
                Optional<PlayerRef> playerOpt = server.getPlayer(playerIdOpt.get());
                if (playerOpt.isPresent()) {
                    kitService.applyKit(playerOpt.get(), kitOpt.get());
                    source.sendMessage(PREFIX + "Kit " + kitOpt.get().displayName() + " applique.");
                }
                yield true;
            }
            case "save" -> {
                if (parts.length < 2) {
                    source.sendMessage(PREFIX + "Usage: /kit save <nom>");
                    yield true;
                }
                Optional<UUID> playerIdOpt = source.playerId();
                if (playerIdOpt.isEmpty()) {
                    source.sendMessage(PREFIX + "Commande joueur uniquement.");
                    yield true;
                }
                String kitId = parts[1].toLowerCase().replace(" ", "_");
                Optional<PlayerRef> playerOpt = server.getPlayer(playerIdOpt.get());
                if (playerOpt.isPresent()) {
                    kitService.saveKitFromPlayer(playerOpt.get(), kitId, kitId);
                    source.sendMessage(PREFIX + "Kit '" + kitId + "' sauvegarde depuis ton inventaire!");
                } else {
                    source.sendMessage(PREFIX + "Erreur: joueur introuvable.");
                }
                yield true;
            }
            case "delete" -> {
                if (parts.length < 2) {
                    source.sendMessage(PREFIX + "Usage: /kit delete <nom>");
                    yield true;
                }
                String kitId = parts[1].toLowerCase();
                if (kitService.deleteKit(kitId)) {
                    source.sendMessage(PREFIX + "Kit '" + kitId + "' supprime.");
                } else {
                    source.sendMessage(PREFIX + "Kit inconnu: " + kitId);
                }
                yield true;
            }
            default -> {
                source.sendMessage(PREFIX + "Usage: /kit list | /kit info <nom> | /kit preview <nom> | /kit save <nom> | /kit delete <nom>");
                yield true;
            }
        };
    }

    private boolean handleStats(CommandSource source, String args) {
        String[] parts = args.split("\\s+", 2);
        String targetName = parts.length > 0 && !parts[0].isEmpty() ? parts[0] : null;

        UUID targetId;
        String displayName;

        if (targetName != null) {
            // Stats d'un autre joueur
            Optional<PlayerRef> targetOpt = server.getPlayerByName(targetName);
            if (targetOpt.isEmpty()) {
                source.sendMessage(PREFIX + "Joueur introuvable: " + targetName);
                return true;
            }
            targetId = targetOpt.get().id();
            displayName = targetOpt.get().name();
        } else {
            // Ses propres stats
            Optional<UUID> playerIdOpt = source.playerId();
            if (playerIdOpt.isEmpty()) {
                source.sendMessage(PREFIX + "Usage: /stats [joueur]");
                return true;
            }
            targetId = playerIdOpt.get();
            displayName = source.player().map(PlayerRef::name).orElse("Toi");
        }

        Optional<PlayerStats> statsOpt = rankingService.getStats(targetId);
        if (statsOpt.isEmpty()) {
            source.sendMessage(PREFIX + displayName + " n'a pas encore joue.");
            return true;
        }

        PlayerStats stats = statsOpt.get();
        int rank = rankingService.getPlayerRank(targetId);

        source.sendMessage(PREFIX + "=== Stats de " + displayName + " ===");
        source.sendMessage("  Rang: " + stats.getRank().displayName() + " (" + stats.elo() + " ELO)");
        source.sendMessage("  Position: #" + rank + " / " + rankingService.getTotalPlayers());
        source.sendMessage("  Victoires: " + stats.wins() + " | Defaites: " + stats.losses());
        source.sendMessage("  Winrate: " + String.format("%.1f%%", stats.winRate()));
        source.sendMessage("  Serie actuelle: " + stats.winStreak() + " | Meilleure: " + stats.bestWinStreak());

        return true;
    }

    private boolean handleLeaderboard(CommandSource source, String args) {
        String[] parts = args.split("\\s+", 2);
        String sortType = parts.length > 0 && !parts[0].isEmpty() ? parts[0].toLowerCase() : "elo";

        List<PlayerStats> leaderboard = switch (sortType) {
            case "wins", "victoires" -> rankingService.getLeaderboardByWins(10);
            case "winrate", "wr" -> rankingService.getLeaderboardByWinRate(10);
            default -> rankingService.getLeaderboard(10);
        };

        String title = switch (sortType) {
            case "wins", "victoires" -> "Top 10 (Victoires)";
            case "winrate", "wr" -> "Top 10 (Winrate)";
            default -> "Top 10 (ELO)";
        };

        source.sendMessage(PREFIX + "=== " + title + " ===");

        if (leaderboard.isEmpty()) {
            source.sendMessage("  Aucun joueur classe.");
            return true;
        }

        int position = 1;
        for (PlayerStats stats : leaderboard) {
            String medal = switch (position) {
                case 1 -> "[1]";
                case 2 -> "[2]";
                case 3 -> "[3]";
                default -> "[" + position + "]";
            };

            String value = switch (sortType) {
                case "wins", "victoires" -> stats.wins() + " wins";
                case "winrate", "wr" -> String.format("%.1f%%", stats.winRate());
                default -> stats.elo() + " ELO";
            };

            source.sendMessage("  " + medal + " " + stats.playerName() + " - " + stats.getRank().displayName() + " - " + value);
            position++;
        }

        source.sendMessage(PREFIX + "Utilise /ranking pour voir le menu complet.");
        return true;
    }
}
