package com.fancyinnovations.fancycore.teleport.service;

import com.fancyinnovations.fancycore.api.teleport.Warp;
import com.fancyinnovations.fancycore.api.teleport.WarpService;
import com.fancyinnovations.fancycore.api.teleport.WarpStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarpServiceImpl implements WarpService {

    private final WarpStorage storage;
    private final Map<String, Warp> warpCache;

    public WarpServiceImpl(WarpStorage storage) {
        this.storage = storage;
        this.warpCache = new HashMap<>();
        load();
    }

    private void load() {
        List<Warp> warps = storage.getAllWarps();
        for (Warp warp : warps) {
            warpCache.put(warp.name(), warp);
        }
    }

    @Override
    public Warp getWarp(String name) {
        return warpCache.get(name);
    }

    @Override
    public List<Warp> getAllWarps() {
        return List.copyOf(warpCache.values());
    }

    @Override
    public void setWarp(Warp warp) {
        warpCache.put(warp.name(), warp);
        storage.storeWarp(warp);
    }

    @Override
    public void deleteWarp(String name) {
        warpCache.remove(name);
        storage.deleteWarp(name);
    }
}
