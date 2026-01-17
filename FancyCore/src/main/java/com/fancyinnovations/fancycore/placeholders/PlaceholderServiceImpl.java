package com.fancyinnovations.fancycore.placeholders;

import com.fancyinnovations.fancycore.api.placeholders.PlaceholderProvider;
import com.fancyinnovations.fancycore.api.placeholders.PlaceholderService;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlaceholderServiceImpl implements PlaceholderService {

    private final Map<String, PlaceholderProvider> providers; // Identifier -> Provider

    public PlaceholderServiceImpl() {
        this.providers = new ConcurrentHashMap<>();
    }

    @Override
    public void registerProvider(PlaceholderProvider provider) {
        if (providers.containsKey(provider.getIdentifier())) {
            throw new IllegalArgumentException("A placeholder provider with the identifier '" + provider.getIdentifier() + "' is already registered.");
        }

        this.providers.put(provider.getIdentifier(), provider);
    }

    @Override
    public void unregisterProvider(PlaceholderProvider provider) {
        this.providers.remove(provider.getIdentifier());
    }

    @Override
    public String parse(FancyPlayer player, String input) {
        // TODO: currently doesn't support parameters in placeholders ("%placeholder:param%")

        String parsedInput = input;
        for (PlaceholderProvider provider : providers.values()) {
            String identifier = provider.getIdentifier();
            String placeholder = "%" + identifier + "%";
            if (parsedInput.contains(placeholder)) {
                String replacement = provider.parse(player, parsedInput);
                parsedInput = parsedInput.replace(placeholder, replacement);
            }
        }

        return parsedInput;
    }
}
