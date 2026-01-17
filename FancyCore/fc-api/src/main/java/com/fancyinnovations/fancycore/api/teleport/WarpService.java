package com.fancyinnovations.fancycore.api.teleport;

import com.fancyinnovations.fancycore.api.FancyCore;

import java.util.List;

public interface WarpService {

    static WarpService get() {
        return FancyCore.get().getWarpService();
    }

    Warp getWarp(String name);

    List<Warp> getAllWarps();

    void setWarp(Warp warp);

    void deleteWarp(String name);

}
