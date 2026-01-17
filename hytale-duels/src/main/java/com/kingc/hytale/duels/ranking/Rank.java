package com.kingc.hytale.duels.ranking;

public enum Rank {
    BRONZE_III("Bronze III", 0, 799, "#cd7f32"),
    BRONZE_II("Bronze II", 800, 899, "#cd7f32"),
    BRONZE_I("Bronze I", 900, 999, "#cd7f32"),
    SILVER_III("Silver III", 1000, 1099, "#c0c0c0"),
    SILVER_II("Silver II", 1100, 1199, "#c0c0c0"),
    SILVER_I("Silver I", 1200, 1299, "#c0c0c0"),
    GOLD_III("Gold III", 1300, 1399, "#ffd700"),
    GOLD_II("Gold II", 1400, 1499, "#ffd700"),
    GOLD_I("Gold I", 1500, 1599, "#ffd700"),
    PLATINUM_III("Platinum III", 1600, 1699, "#00cec9"),
    PLATINUM_II("Platinum II", 1700, 1799, "#00cec9"),
    PLATINUM_I("Platinum I", 1800, 1899, "#00cec9"),
    DIAMOND_III("Diamond III", 1900, 1999, "#74b9ff"),
    DIAMOND_II("Diamond II", 2000, 2099, "#74b9ff"),
    DIAMOND_I("Diamond I", 2100, 2199, "#74b9ff"),
    MASTER("Master", 2200, 2399, "#a29bfe"),
    GRANDMASTER("Grandmaster", 2400, 2599, "#fd79a8"),
    CHAMPION("Champion", 2600, Integer.MAX_VALUE, "#e84393");

    private final String displayName;
    private final int minElo;
    private final int maxElo;
    private final String color;

    Rank(String displayName, int minElo, int maxElo, String color) {
        this.displayName = displayName;
        this.minElo = minElo;
        this.maxElo = maxElo;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public int minElo() {
        return minElo;
    }

    public int maxElo() {
        return maxElo;
    }

    public String color() {
        return color;
    }

    public int eloToNextRank() {
        if (this == CHAMPION) {
            return 0;
        }
        return maxElo + 1;
    }

    public static Rank fromElo(int elo) {
        for (Rank rank : values()) {
            if (elo >= rank.minElo && elo <= rank.maxElo) {
                return rank;
            }
        }
        return BRONZE_III;
    }

    public Rank next() {
        int ordinal = this.ordinal();
        if (ordinal < values().length - 1) {
            return values()[ordinal + 1];
        }
        return this;
    }

    public Rank previous() {
        int ordinal = this.ordinal();
        if (ordinal > 0) {
            return values()[ordinal - 1];
        }
        return this;
    }
}
