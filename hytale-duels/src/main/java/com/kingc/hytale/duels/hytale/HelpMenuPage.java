package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class HelpMenuPage extends InteractiveCustomUIPage<HelpMenuPage.HelpEventData> {
    private static final String PAGE_UI = "Pages/HelpMenu.ui";

    private final HytaleDuelsPlugin plugin;

    public HelpMenuPage(HytaleDuelsPlugin plugin, PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, HelpEventData.CODEC);
        this.plugin = plugin;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events, Store<EntityStore> store) {
        commands.append(PAGE_UI);
        buildCommandsList(commands);
        buildRulesList(commands);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, HelpEventData data) {
        // Static page, no events
    }

    private void buildCommandsList(UICommandBuilder commands) {
        commands.clear("#CommandsList");
        
        String[][] cmds = {
            {"/duel <joueur>", "Defier un joueur"},
            {"/duel accept", "Accepter un defi"},
            {"/duel decline", "Refuser un defi"},
            {"/queue 1v1 [kit]", "File 1v1"},
            {"/queue 2v2 [kit]", "File 2v2"},
            {"/queue leave", "Quitter la file"},
            {"/kit list", "Lister les kits"},
            {"/kit save <nom>", "Sauvegarder kit"},
            {"/stats", "Voir stats"},
            {"/ranking", "Menu classement"}
        };
        
        for (String[] cmd : cmds) {
            String entry = "Label { Text: \"" + escapeUi(cmd[0] + " - " + cmd[1]) + "\"; " +
                "Style: (FontSize: 12, TextColor: #d7e0e8); Anchor: (Bottom: 4); }";
            commands.appendInline("#CommandsList", entry);
        }
    }

    private void buildRulesList(UICommandBuilder commands) {
        commands.clear("#RulesList");
        
        String[] rules = {
            "- 5 minutes max par match",
            "- Match nul si le temps expire",
            "- ELO ajuste automatiquement"
        };
        
        for (String rule : rules) {
            String entry = "Label { Text: \"" + escapeUi(rule) + "\"; " +
                "Style: (FontSize: 12, TextColor: #a9b7c4); Anchor: (Bottom: 4); }";
            commands.appendInline("#RulesList", entry);
        }
    }

    private String escapeUi(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static final class HelpEventData {
        static final String KEY_ACTION = "Action";

        static final BuilderCodec<HelpEventData> CODEC = BuilderCodec.builder(HelpEventData.class, HelpEventData::new)
            .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), HelpEventData::setAction, HelpEventData::getAction).add()
            .build();

        private String action;

        public HelpEventData() {}

        public String getAction() { return action; }
        private void setAction(String action) { this.action = action; }
    }
}
