package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kingc.hytale.duels.DuelsPlugin;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class DuelsMenuPage extends InteractiveCustomUIPage<Object> {
    private final DuelsPlugin plugin;

    public DuelsMenuPage(HytaleDuelsPlugin hytalePlugin, PlayerRef player) {
        // Passing null for codec as a workaround since generic type inference is failing without docs.
        // In a real scenario, we would define a proper Codec for the data type.
        super(player, com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime.CanDismiss, null);
        this.plugin = hytalePlugin.core();
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder event, Store<EntityStore> store) {
        // UI logic commented out until API methods are verified
        // cmd.page("Pages/DuelsMenu.ui");
    }
}
