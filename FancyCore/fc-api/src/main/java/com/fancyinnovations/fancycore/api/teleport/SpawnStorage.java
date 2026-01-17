package com.fancyinnovations.fancycore.api.teleport;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface SpawnStorage {

    @ApiStatus.Internal
    Location loadSpawnLocation();

    @ApiStatus.Internal
    void storeSpawnLocation(Location location);

    @ApiStatus.Internal
    void deleteSpawnLocation();

}
