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
        // Simple constructor without codec for now, passing null as codec if
        // compilation allows,
        // or effectively we don't bind data for this static menu.
        super(player, com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime.CanDismiss, null);
        this.plugin = hytalePlugin.core();
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder event, Store<EntityStore> store) {
        cmd.append("Pages/DuelsMenu.ui");

        // Build stats
        // Build queue status
        // Bind actions

        event.addEventBinding(com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType.Activating,
                "#btnClose", com.hypixel.hytale.server.core.ui.builder.EventData.of("Button", "action:close"), false);
        event.addEventBinding(com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType.Activating,
                "#btnQueue1v1", com.hypixel.hytale.server.core.ui.builder.EventData.of("Button", "action:queue_1v1"),
                false);
        // ...
    }

    // @Override
    // public void handleDataEvent(...)
}
