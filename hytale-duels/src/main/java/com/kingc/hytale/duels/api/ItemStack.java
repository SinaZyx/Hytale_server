package com.kingc.hytale.duels.api;

import java.util.Map;

public record ItemStack(
    String itemId,
    int count,
    Map<String, Object> metadata
) {
    public ItemStack(String itemId, int count) {
        this(itemId, count, Map.of());
    }

    public ItemStack(String itemId) {
        this(itemId, 1, Map.of());
    }
}
