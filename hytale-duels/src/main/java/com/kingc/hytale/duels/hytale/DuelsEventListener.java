package com.kingc.hytale.duels.hytale;

// import com.hypixel.hytale.server.core.event.events.player.PlayerDeathEvent;
import com.kingc.hytale.duels.DuelsPlugin;

public class DuelsEventListener {
    private final DuelsPlugin core;

    public DuelsEventListener(DuelsPlugin core) {
        this.core = core;
    }

    // public void onPlayerDeath(PlayerDeathEvent event) {
    //    var player = event.getPlayer();
    //    if (player != null) {
    //        core.onPlayerDeath(player.getUuid());
    //    }
    // }
}
