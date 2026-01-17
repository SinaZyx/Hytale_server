package com.fancyinnovations.fancycore.api.events.service;

import com.fancyinnovations.fancycore.api.FancyCore;
import com.fancyinnovations.fancycore.api.events.FancyEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Service interface for managing and firing events within the FancyCore system.
 */
public interface EventService {

    static EventService get() {
        return FancyCore.get().getEventService();
    }

    /**
     * Fires an event to all registered listeners.
     *
     * @param event the event to fire
     * @return true if the event was not cancelled, false otherwise
     */
    @ApiStatus.Internal
    boolean fireEvent(FancyEvent event);

    /**
     * Registers a listener for a specific event type.
     *
     * @param event    the event class to listen for
     * @param listener the listener to register
     */
    <T extends FancyEvent> void registerListener(Class<T> event, EventListener<T> listener);

}
