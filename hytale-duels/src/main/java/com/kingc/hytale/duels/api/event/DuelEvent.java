package com.kingc.hytale.duels.api.event;

public abstract class DuelEvent {
    private final long timestamp;

    public DuelEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }
}
