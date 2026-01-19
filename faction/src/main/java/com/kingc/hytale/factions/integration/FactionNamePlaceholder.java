package com.kingc.hytale.factions.integration;

import com.fancyinnovations.fancycore.api.placeholders.PlaceholderProvider;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerData;
import com.kingc.hytale.factions.FactionsPlugin;
import com.kingc.hytale.factions.model.Faction;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class FactionNamePlaceholder implements PlaceholderProvider {

    private final Supplier<FactionsPlugin> pluginSupplier;

    public FactionNamePlaceholder(Supplier<FactionsPlugin> pluginSupplier) {
        this.pluginSupplier = pluginSupplier;
    }

    @Override
    public String getName() {
        return "Faction Name";
    }

    @Override
    public String getIdentifier() {
        return "faction_name";
    }

    @Override
    public String parse(FancyPlayer player, String input) {
        if (player == null) {
            return "";
        }
        FancyPlayerData data = player.getData();
        if (data == null) {
            return "";
        }
        UUID playerId = data.getUUID();
        if (playerId == null) {
            return "";
        }
        FactionsPlugin plugin = pluginSupplier.get();
        if (plugin == null) {
            return "";
        }
        Optional<Faction> faction = plugin.service().findFactionByMember(playerId);
        if (faction.isPresent()) {
            return faction.get().name();
        }
        return "";
    }
}
