package com.fancyinnovations.fancycore.events;

import com.fancyinnovations.fancycore.api.FancyCoreConfig;
import com.fancyinnovations.fancycore.api.discord.Message;
import com.fancyinnovations.fancycore.api.events.FancyEvent;
import com.fancyinnovations.fancycore.api.events.service.EventListener;
import com.fancyinnovations.fancycore.api.events.service.EventService;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.utils.DiscordWebhook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventServiceImpl implements EventService {

    public static final FancyCoreConfig CONFIG = FancyCorePlugin.get().getConfig();

    private final Map<Class<? extends FancyEvent>, List<EventListener<? extends FancyEvent>>> listeners;

    public EventServiceImpl() {
        this.listeners = new ConcurrentHashMap<>();
    }

    @Override
    public boolean fireEvent(FancyEvent event) {
        if (!CONFIG.getEventDiscordWebhookUrl().isEmpty()) {
            for (String evt : CONFIG.getEventDiscordNotifications()) {
                if (event.getClass().getSimpleName().equalsIgnoreCase(evt)) {
                    Message discordMessage = event.getDiscordMessage();
                    if (discordMessage != null) {
                        DiscordWebhook.sendMsg(CONFIG.getEventDiscordWebhookUrl(), discordMessage);
                    }

                    break;
                }
            }
        }

        if (!this.listeners.containsKey(event.getClass())) {
            return true;
        }

        for (EventListener<? extends FancyEvent> listener : this.listeners.get(event.getClass())) {
            ((EventListener<FancyEvent>) listener).on(event);

            if (event.isCancellable() && event.isCancelled()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public <T extends FancyEvent> void registerListener(Class<T> event, EventListener<T> listener) {
        if (this.listeners.containsKey(event)) {
            this.listeners.get(event).add(listener);
        } else {
            List<EventListener<? extends FancyEvent>> listeners = new ArrayList<>();
            this.listeners.put(event, listeners);
        }
    }
}
