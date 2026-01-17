package com.kingc.hytale.duels.ranking;

public final class EloCalculator {
    // Facteur K : influence l'amplitude des changements d'ELO
    // Plus K est eleve, plus les changements sont importants
    private static final int K_FACTOR_NEW = 40;      // Nouveaux joueurs (< 30 matchs)
    private static final int K_FACTOR_NORMAL = 32;   // Joueurs normaux
    private static final int K_FACTOR_VETERAN = 24;  // Veterans (> 100 matchs)
    private static final int K_FACTOR_ELITE = 16;    // Elite (> 2200 ELO)

    // Bonus/malus
    private static final int WIN_STREAK_BONUS = 5;   // Bonus par victoire consecutive (max 3)
    private static final int UPSET_BONUS = 10;       // Bonus si victoire contre joueur +200 ELO

    private EloCalculator() {}

    /**
     * Calcule le changement d'ELO apres un match
     * @param winnerElo ELO du gagnant avant le match
     * @param loserElo ELO du perdant avant le match
     * @param winnerMatches Nombre total de matchs du gagnant
     * @param winnerStreak Serie de victoires actuelle du gagnant
     * @return EloChange contenant les changements pour le gagnant et le perdant
     */
    public static EloChange calculate(int winnerElo, int loserElo, int winnerMatches, int winnerStreak) {
        // Probabilite attendue de victoire
        double expectedWinner = expectedScore(winnerElo, loserElo);
        double expectedLoser = 1.0 - expectedWinner;

        // Determiner le facteur K
        int kFactorWinner = getKFactor(winnerElo, winnerMatches);
        int kFactorLoser = getKFactor(loserElo, 50); // Assume 50 matchs pour le perdant (simplifie)

        // Calcul de base
        int winnerGain = (int) Math.round(kFactorWinner * (1.0 - expectedWinner));
        int loserLoss = (int) Math.round(kFactorLoser * expectedLoser);

        // Bonus de serie (max 3 victoires consecutives comptent)
        int streakBonus = Math.min(winnerStreak, 3) * WIN_STREAK_BONUS;
        winnerGain += streakBonus;

        // Bonus upset (victoire contre joueur beaucoup plus fort)
        int eloDiff = loserElo - winnerElo;
        if (eloDiff >= 200) {
            winnerGain += UPSET_BONUS;
        } else if (eloDiff >= 100) {
            winnerGain += UPSET_BONUS / 2;
        }

        // Minimum garanti
        winnerGain = Math.max(winnerGain, 5);
        loserLoss = Math.max(loserLoss, 5);

        return new EloChange(winnerGain, -loserLoss);
    }

    /**
     * Calcule le changement d'ELO pour un match 2v2
     * La moyenne des ELO des equipes est utilisee
     */
    public static EloChange calculate2v2(int team1Elo1, int team1Elo2, int team2Elo1, int team2Elo2, boolean team1Won) {
        int team1AvgElo = (team1Elo1 + team1Elo2) / 2;
        int team2AvgElo = (team2Elo1 + team2Elo2) / 2;

        if (team1Won) {
            return calculate(team1AvgElo, team2AvgElo, 50, 0);
        } else {
            EloChange result = calculate(team2AvgElo, team1AvgElo, 50, 0);
            // Inverser pour que winnerChange soit pour team2
            return new EloChange(-result.loserChange(), -result.winnerChange());
        }
    }

    private static double expectedScore(int playerElo, int opponentElo) {
        return 1.0 / (1.0 + Math.pow(10.0, (opponentElo - playerElo) / 400.0));
    }

    private static int getKFactor(int elo, int totalMatches) {
        if (elo >= 2200) {
            return K_FACTOR_ELITE;
        }
        if (totalMatches < 30) {
            return K_FACTOR_NEW;
        }
        if (totalMatches > 100) {
            return K_FACTOR_VETERAN;
        }
        return K_FACTOR_NORMAL;
    }

    public record EloChange(int winnerChange, int loserChange) {
        public int absoluteChange() {
            return Math.abs(winnerChange);
        }
    }
}
