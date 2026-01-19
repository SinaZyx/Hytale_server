package com.kingc.hytale.factions.api;

import com.kingc.hytale.factions.api.event.FactionEventBus;
import com.kingc.hytale.factions.model.ClaimKey;
import com.kingc.hytale.factions.model.Faction;
import com.kingc.hytale.factions.model.War;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FactionsApi {
    Optional<Faction> getFactionById(UUID factionId);

    Optional<Faction> getFactionByName(String name);

    Optional<Faction> getFactionByMember(UUID playerId);

    Collection<Faction> getFactions();

    Optional<UUID> getClaimOwnerId(ClaimKey claim);

    Optional<UUID> getClaimOwnerId(Location location);

    Optional<War> getActiveWar(UUID factionId);

    List<War> getWarHistory();

    Optional<Double> getTreasuryBalance(UUID factionId);

    FactionEventBus events();
}
