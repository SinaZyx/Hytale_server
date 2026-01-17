package com.kingc.hytale.factions.model;

import com.kingc.hytale.factions.api.Location;

import java.util.Objects;

public final class ClaimKey {
    private static final String SEP = "|";

    private final String world;
    private final int x;
    private final int z;

    public ClaimKey(String world, int x, int z) {
        this.world = Objects.requireNonNull(world, "world");
        this.x = x;
        this.z = z;
    }

    public String world() {
        return world;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    public String toKey() {
        return world + SEP + x + SEP + z;
    }

    public static ClaimKey fromKey(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Invalid claim key: " + raw);
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid claim key: " + raw);
        }
        String world = parts[0];
        int x = Integer.parseInt(parts[1]);
        int z = Integer.parseInt(parts[2]);
        return new ClaimKey(world, x, z);
    }

    public static ClaimKey fromLocation(Location location, int chunkSize) {
        int cx = chunkCoord(location.x(), chunkSize);
        int cz = chunkCoord(location.z(), chunkSize);
        return new ClaimKey(location.world(), cx, cz);
    }

    private static int chunkCoord(double value, int chunkSize) {
        return (int) Math.floor(value / chunkSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClaimKey claimKey = (ClaimKey) o;
        return x == claimKey.x && z == claimKey.z && world.equals(claimKey.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, z);
    }

    @Override
    public String toString() {
        return "ClaimKey{" + "world='" + world + '\'' + ", x=" + x + ", z=" + z + '}';
    }
}
