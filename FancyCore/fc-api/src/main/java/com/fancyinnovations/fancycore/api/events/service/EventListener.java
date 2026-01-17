package com.fancyinnovations.fancycore.api.events.service;

import com.fancyinnovations.fancycore.api.events.FancyEvent;

/**
 * A listener interface for handling FancyEvent instances.
 *
 * @param <T> the type of FancyEvent this listener handles
 */
public interface EventListener<T extends FancyEvent> {

    /**
     * Called when the event this listener is registered for is fired.
     *
     * @param event the event instance
     */
    void on(T event);

}
