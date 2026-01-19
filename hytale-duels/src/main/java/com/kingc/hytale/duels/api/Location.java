package com.kingc.hytale.duels.api;

public record Location(String world, double x, double y, double z, float yaw, float pitch) {
    public Location(String world, double x, double y, double z) {
        this(world, x, y, z, 0f, 0f);
    }
}
