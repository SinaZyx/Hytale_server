package com.kingc.hytale.factions.integration;

import com.fancyinnovations.fancycore.api.placeholders.PlaceholderProvider;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.kingc.hytale.factions.hytale.HytaleFactionsPlugin;
import com.kingc.hytale.factions.model.Faction;

import java.util.Optional;
import java.util.UUID;

public class FactionNamePlaceholder implements PlaceholderProvider {

    private final HytaleFactionsPlugin plugin;

    public FactionNamePlaceholder(HytaleFactionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "faction_name";
    }

    @Override
    public String onPlaceholderRequest(FancyPlayer player, String params) {
        if (player == null || !player.isOnline()) {
            return "";
        }

        UUID playerId = player.getUUID();
        // Assuming access to FactionsPlugin service via HytaleFactionsPlugin helper
        // We might need to expose the service more cleanly, but code showed 'core()' method accessing 'plugin'.
        if (plugin.core() == null || plugin.core().service() == null) {
            return "";
        }

        Optional<Faction> factionOpt = plugin.core().service().findFactionByMember(playerId);
        return factionOpt.map(f -> "&a[" + f.name() + "]&r ").orElse("");
    }
}
