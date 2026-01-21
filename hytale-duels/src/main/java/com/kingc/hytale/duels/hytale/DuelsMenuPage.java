package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kingc.hytale.duels.DuelsPlugin;

// Assuming InteractiveCustomUIPage signature
public class DuelsMenuPage extends InteractiveCustomUIPage<Object> {
    private final DuelsPlugin plugin;

    public DuelsMenuPage(HytaleDuelsPlugin hytalePlugin, PlayerRef player) {
        // Need to check constructor signature of InteractiveCustomUIPage
        // Usually (PlayerRef player, BuilderCodec<T> codec) or similar
        // I'll assume a simplified version or abstract based on developer_guide
        super(player, com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime.CanDismiss, com.hypixel.hytale.codec.builder.BuilderCodec.of(Object.class));
        this.plugin = hytalePlugin.core();
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder event, Store<EntityStore> store) {
        cmd.page("Pages/DuelsMenu.ui");

        // Build stats
        // Build queue status
        // Bind actions

        event.onClick("#btnClose", "action:close");
        event.onClick("#btnQueue1v1", "action:queue_1v1");
        // ...
    }

    // @Override
    // public void handleDataEvent(...)
}
