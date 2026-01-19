package com.fancyinnovations.fancycore.api.placeholders;

import com.fancyinnovations.fancycore.api.FancyCore;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Service for managing and parsing placeholders within strings.
 */
public interface PlaceholderService {

    static PlaceholderService get() {
        return FancyCore.get().getPlaceholderService();
    }

    /**
     * Registers a new placeholder provider.
     *
     * @param provider The placeholder provider to register.
     */
    void registerProvider(PlaceholderProvider provider);

    /**
     * Unregisters an existing placeholder provider.
     *
     * @param provider The placeholder provider to unregister.
     */
    void unregisterProvider(PlaceholderProvider provider);

    /**
     * Parses the input string, replacing all recognized placeholders with their corresponding values.
     *
     * @param input The input string containing placeholders.
     * @return The parsed string with placeholders replaced.
     */
    String parse(@Nullable FancyPlayer player, String input);

    default String parse(String input) {
        return parse(null, input);
    }

}
