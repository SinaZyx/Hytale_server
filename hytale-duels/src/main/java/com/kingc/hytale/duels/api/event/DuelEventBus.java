package com.kingc.hytale.duels.api.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DuelEventBus {
    private final Map<Class<?>, List<Consumer<DuelEvent>>> handlers = new ConcurrentHashMap<>();

    public void fire(DuelEvent event) {
        List<Consumer<DuelEvent>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (Consumer<DuelEvent> handler : eventHandlers) {
                try {
                    handler.accept(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends DuelEvent> void register(Class<T> eventClass, Consumer<T> handler) {
        handlers.computeIfAbsent(eventClass, k -> new ArrayList<>())
                .add((Consumer<DuelEvent>) handler);
    }
}
