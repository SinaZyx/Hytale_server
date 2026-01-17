package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Locale;

public final class HelpMenuPage extends InteractiveCustomUIPage<HelpMenuPage.HelpEventData> {
    private static final String PAGE_UI = "Pages/HelpMenu.ui";

    private final HytaleDuelsPlugin plugin;
    private String activeTab = "commands";

    public HelpMenuPage(HytaleDuelsPlugin plugin, PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, HelpEventData.CODEC);
        this.plugin = plugin;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events, Store<EntityStore> store) {
        commands.append(PAGE_UI);
        applyTabState(commands);
        buildContent(commands);
        bindActions(events);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, HelpEventData data) {
        if (data == null) return;
        String action = normalize(data.getAction());
        if (action == null) return;
        action = action.toLowerCase(Locale.ROOT);

        switch (action) {
            case "tab_commands" -> activeTab = "commands";
            case "tab_kits" -> activeTab = "kits";
            case "tab_rules" -> activeTab = "rules";
        }
        refresh();
    }

    private void refresh() {
        UICommandBuilder commands = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        applyTabState(commands);
        buildContent(commands);
        bindActions(events);
        sendUpdate(commands, events, false);
    }

    private void applyTabState(UICommandBuilder commands) {
        boolean isCommands = "commands".equals(activeTab);
        boolean isKits = "kits".equals(activeTab);
        boolean isRules = "rules".equals(activeTab);

        commands.set("#CommandsView.Visible", isCommands);
        commands.set("#KitsView.Visible", isKits);
        commands.set("#RulesView.Visible", isRules);

        commands.set("#NavCommandsIndicator.Visible", isCommands);
        commands.set("#NavKitsIndicator.Visible", isKits);
        commands.set("#NavRulesIndicator.Visible", isRules);
    }

    private void buildContent(UICommandBuilder commands) {
        switch (activeTab) {
            case "commands" -> buildCommandsTab(commands);
            case "kits" -> buildKitsTab(commands);
            case "rules" -> buildRulesTab(commands);
        }
    }

    private void buildCommandsTab(UICommandBuilder commands) {
        commands.clear("#CommandsList");

        String[][] cmds = {
            {"/duel <joueur>", "Defier un joueur en 1v1"},
            {"/duel accept", "Accepter un defi"},
            {"/duel decline", "Refuser un defi"},
            {"/queue 1v1 [kit]", "Rejoindre la file 1v1"},
            {"/queue 2v2 [kit]", "Rejoindre la file 2v2"},
            {"/queue leave", "Quitter la file"},
            {"/queue status", "Voir ton statut"},
            {"/kit list", "Lister les kits"},
            {"/kit preview <nom>", "Essayer un kit"},
            {"/kit save <nom>", "Sauvegarder ton inventaire comme kit"},
            {"/stats [joueur]", "Voir les statistiques"},
            {"/top [elo|wins|wr]", "Voir le classement"},
            {"/ranking", "Ouvrir le menu de classement"}
        };

        for (String[] cmd : cmds) {
            String entry = "HorizontalLayout { Style: (BackgroundColor: #2a2a2a, Padding: 6); Anchor: (Bottom: 2); Children: [ "
                + "Label { Text: \"" + escapeUiText(cmd[0]) + "\"; Style: (FontSize: 12, TextColor: #4fc3f7, Width: 180); } "
                + "Label { Text: \"" + escapeUiText(cmd[1]) + "\"; Style: (FontSize: 11, TextColor: #d7e0e8); } "
                + "]; }";
            commands.appendInline("#CommandsList", entry);
        }
    }

    private void buildKitsTab(UICommandBuilder commands) {
        commands.clear("#KitsList");

        var kits = plugin.core().kitService().getAllKits();
        if (kits.isEmpty()) {
            commands.appendInline("#KitsList",
                "Label { Text: \"Aucun kit disponible\"; Style: (Alignment: Center, FontSize: 12, TextColor: #9aa7b0); }");
            return;
        }

        for (var kit : kits) {
            int itemCount = kit.items() != null ? kit.items().size() : 0;
            String entry = "HorizontalLayout { Style: (BackgroundColor: #2a2a2a, Padding: 6); Anchor: (Bottom: 2); Children: [ "
                + "Label { Text: \"" + escapeUiText(kit.displayName()) + "\"; Style: (FontSize: 13, TextColor: #4fc3f7, Width: 120); } "
                + "Label { Text: \"" + itemCount + " items\"; Style: (FontSize: 11, TextColor: #9aa7b0, Width: 80); } "
                + "Label { Text: \"/kit preview " + kit.id() + "\"; Style: (FontSize: 10, TextColor: #6c7a89); } "
                + "]; }";
            commands.appendInline("#KitsList", entry);
        }
    }

    private void buildRulesTab(UICommandBuilder commands) {
        commands.clear("#RulesList");

        String[] rules = {
            "1. Pas de triche ou d'exploit",
            "2. Respecte les adversaires",
            "3. Temps de match: 5 minutes max",
            "4. Match nul si le temps expire",
            "5. ELO calcule automatiquement",
            "6. Les kits sont fournis au debut"
        };

        for (String rule : rules) {
            String entry = "Label { Text: \"" + escapeUiText(rule) + "\"; Style: (FontSize: 12, TextColor: #d7e0e8, Padding: 4); }";
            commands.appendInline("#RulesList", entry);
        }
    }

    private void bindActions(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavCommandsButton",
            EventData.of(HelpEventData.KEY_ACTION, "tab_commands"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavKitsButton",
            EventData.of(HelpEventData.KEY_ACTION, "tab_kits"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavRulesButton",
            EventData.of(HelpEventData.KEY_ACTION, "tab_rules"));
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String escapeUiText(String value) {
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
