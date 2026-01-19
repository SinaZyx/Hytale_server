package com.kingc.hytale.factions.api.event;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class FactionEventBus {
    private final Map<Class<? extends FactionEvent>, CopyOnWriteArrayList<Consumer<? extends FactionEvent>>> listeners =
            new ConcurrentHashMap<>();

    public <T extends FactionEvent> void register(Class<T> type, Consumer<T> listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listener, "listener");
        listeners.computeIfAbsent(type, ignored -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public <T extends FactionEvent> void unregister(Class<T> type, Consumer<T> listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listener, "listener");
        CopyOnWriteArrayList<Consumer<? extends FactionEvent>> list = listeners.get(type);
        if (list == null) {
            return;
        }
        list.remove(listener);
        if (list.isEmpty()) {
            listeners.remove(type);
        }
    }

    public void post(FactionEvent event) {
        if (event == null) {
            return;
        }
        CopyOnWriteArrayList<Consumer<? extends FactionEvent>> list = listeners.get(event.getClass());
        if (list == null) {
            return;
        }
        for (Consumer<? extends FactionEvent> listener : list) {
            dispatch(listener, event);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends FactionEvent> void dispatch(Consumer<? extends FactionEvent> listener, FactionEvent event) {
        ((Consumer<T>) listener).accept((T) event);
    }
}
