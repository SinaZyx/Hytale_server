package com.fancyinnovations.fancycore.api.teleport;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public interface WarpStorage {

    @ApiStatus.Internal
    Warp getWarp(String name);

    @ApiStatus.Internal
    List<Warp> getAllWarps();

    @ApiStatus.Internal
    void storeWarp(Warp warp);

    @ApiStatus.Internal
    void deleteWarp(String name);

}
