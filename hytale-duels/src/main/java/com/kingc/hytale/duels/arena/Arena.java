package com.kingc.hytale.duels.arena;

import com.kingc.hytale.duels.api.Location;

import java.util.List;

public record Arena(
    String id,
    String displayName,
    List<Location> team1Spawns,
    List<Location> team2Spawns,
    Location spectatorSpawn,
    int maxPlayers
) {
    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private String displayName;
        private List<Location> team1Spawns = List.of();
        private List<Location> team2Spawns = List.of();
        private Location spectatorSpawn;
        private int maxPlayers = 2;

        private Builder(String id) {
            this.id = id;
            this.displayName = id;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder team1Spawns(List<Location> spawns) {
            this.team1Spawns = List.copyOf(spawns);
            return this;
        }

        public Builder team2Spawns(List<Location> spawns) {
            this.team2Spawns = List.copyOf(spawns);
            return this;
        }

        public Builder spectatorSpawn(Location spawn) {
            this.spectatorSpawn = spawn;
            return this;
        }

        public Builder maxPlayers(int maxPlayers) {
            this.maxPlayers = maxPlayers;
            return this;
        }

        public Arena build() {
            return new Arena(id, displayName, team1Spawns, team2Spawns, spectatorSpawn, maxPlayers);
        }
    }
}
