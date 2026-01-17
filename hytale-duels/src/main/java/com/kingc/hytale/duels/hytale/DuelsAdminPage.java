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
import com.kingc.hytale.duels.arena.Arena;
import com.kingc.hytale.duels.kit.KitDefinition;
import com.kingc.hytale.duels.match.Match;
import com.kingc.hytale.duels.match.MatchState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class DuelsAdminPage extends InteractiveCustomUIPage<DuelsAdminPage.AdminEventData> {
    private static final String PAGE_UI = "Pages/DuelsAdmin.ui";
    private static final String PREFIX = "[Duels Admin] ";

    private final HytaleDuelsPlugin plugin;
    private String activeTab = "kits";
    private String selectedKit = null;
    private String selectedArena = null;

    public DuelsAdminPage(HytaleDuelsPlugin plugin, PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminEventData.CODEC);
        this.plugin = plugin;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events, Store<EntityStore> store) {
        commands.append(PAGE_UI);
        updateHeader(commands);
        applyTabState(commands);
        buildContent(commands);
        bindActions(events);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, AdminEventData data) {
        if (data == null) {
            return;
        }
        String action = normalize(data.getAction());
        if (action == null) {
            return;
        }
        action = action.toLowerCase(Locale.ROOT);

        if (action.startsWith("tab_")) {
            selectTab(action.substring(4));
            selectedKit = null;
            selectedArena = null;
            refresh();
            return;
        }

        switch (action) {
            // Kit actions
            case "kit_create" -> handleKitCreate(data);
            case "kit_select" -> handleKitSelect(data);
            case "kit_delete" -> handleKitDelete();
            case "kit_save" -> handleKitSave(data);

            // Arena actions
            case "arena_create" -> handleArenaCreate(data);
            case "arena_select" -> handleArenaSelect(data);
            case "arena_delete" -> handleArenaDelete();
            case "arena_setspawn1" -> handleArenaSetSpawn(1);
            case "arena_setspawn2" -> handleArenaSetSpawn(2);

            // Match actions
            case "match_end" -> handleMatchEnd(data);
        }
        refresh();
    }

    private void refresh() {
        UICommandBuilder commands = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        updateHeader(commands);
        applyTabState(commands);
        buildContent(commands);
        bindActions(events);
        sendUpdate(commands, events, false);
    }

    private void updateHeader(UICommandBuilder commands) {
        int kitCount = plugin.core().kitService().getAllKits().size();
        int arenaCount = plugin.core().arenaService().getAllArenas().size();
        long matchCount = getActiveMatchCount();

        String stats = "Kits: " + kitCount + " | Arenes: " + arenaCount + " | Matchs actifs: " + matchCount;
        commands.set("#AdminStats.TextSpans", Message.raw(stats));
    }

    private void applyTabState(UICommandBuilder commands) {
        boolean kits = "kits".equals(activeTab);
        boolean arenas = "arenas".equals(activeTab);
        boolean matches = "matches".equals(activeTab);

        commands.set("#KitsView.Visible", kits);
        commands.set("#ArenasView.Visible", arenas);
        commands.set("#MatchesView.Visible", matches);

        commands.set("#NavKitsIndicator.Visible", kits);
        commands.set("#NavArenasIndicator.Visible", arenas);
        commands.set("#NavMatchesIndicator.Visible", matches);

        commands.set("#KitEditPanel.Visible", kits && selectedKit != null);
        commands.set("#ArenaEditPanel.Visible", arenas && selectedArena != null);
    }

    private void buildContent(UICommandBuilder commands) {
        switch (activeTab) {
            case "kits" -> buildKitsList(commands);
            case "arenas" -> buildArenasList(commands);
            case "matches" -> buildMatchesList(commands);
        }
    }

    private void buildKitsList(UICommandBuilder commands) {
        commands.clear("#KitsList");
        List<KitDefinition> kits = new ArrayList<>(plugin.core().kitService().getAllKits());
        kits.sort(Comparator.comparing(k -> k.displayName().toLowerCase(Locale.ROOT)));

        if (kits.isEmpty()) {
            commands.appendInline("#KitsList",
                "Label { Text: \"Aucun kit\"; Style: (Alignment: Center, FontSize: 12, TextColor: #9aa7b0); }");
            return;
        }

        for (KitDefinition kit : kits) {
            boolean isSelected = kit.id().equals(selectedKit);
            String color = isSelected ? "#4fc3f7" : "#d7e0e8";
            String itemCount = kit.items() != null ? String.valueOf(kit.items().size()) : "0";
            String line = kit.displayName() + " (" + kit.id() + ") - " + itemCount + " items";
            String entry = "Label { Text: \"" + escapeUiText(line) + "\"; Style: (FontSize: 13, TextColor: " + color + "); Anchor: (Bottom: 6); }";
            commands.appendInline("#KitsList", entry);
        }

        if (selectedKit != null) {
            plugin.core().kitService().getKit(selectedKit).ifPresent(kit -> {
                commands.set("#KitNameInput.Value", kit.displayName());
                commands.set("#KitIconInput.Value", kit.iconItem() != null ? kit.iconItem() : "");
            });
        }
    }

    private void buildArenasList(UICommandBuilder commands) {
        commands.clear("#ArenasList");
        List<Arena> arenas = new ArrayList<>(plugin.core().arenaService().getAllArenas());
        arenas.sort(Comparator.comparing(a -> a.displayName().toLowerCase(Locale.ROOT)));

        if (arenas.isEmpty()) {
            commands.appendInline("#ArenasList",
                "Label { Text: \"Aucune arene\"; Style: (Alignment: Center, FontSize: 12, TextColor: #9aa7b0); }");
            return;
        }

        for (Arena arena : arenas) {
            boolean isSelected = arena.id().equals(selectedArena);
            boolean available = plugin.core().arenaService().isArenaAvailable(arena.id());
            String textColor = isSelected ? "#4fc3f7" : "#d7e0e8";
            String status = available ? "Libre" : "Occupee";
            String line = arena.displayName() + " (" + arena.id() + ") - " + status + " - Max: " + arena.maxPlayers();
            String entry = "Label { Text: \"" + escapeUiText(line) + "\"; Style: (FontSize: 13, TextColor: " + textColor + "); Anchor: (Bottom: 6); }";
            commands.appendInline("#ArenasList", entry);
        }

        if (selectedArena != null) {
            plugin.core().arenaService().getArena(selectedArena).ifPresent(arena -> {
                commands.set("#ArenaNameInput.Value", arena.displayName());
                commands.set("#ArenaMaxPlayersInput.Value", String.valueOf(arena.maxPlayers()));
                int spawn1Count = arena.team1Spawns() != null ? arena.team1Spawns().size() : 0;
                int spawn2Count = arena.team2Spawns() != null ? arena.team2Spawns().size() : 0;
                commands.set("#ArenaSpawnsInfo.TextSpans",
                    Message.raw("Spawns Team1: " + spawn1Count + " | Spawns Team2: " + spawn2Count));
            });
        }
    }

    private void buildMatchesList(UICommandBuilder commands) {
        commands.clear("#MatchesList");

        List<Match> matches = getActiveMatches();

        if (matches.isEmpty()) {
            commands.appendInline("#MatchesList",
                "Label { Text: \"Aucun match en cours\"; Style: (Alignment: Center, FontSize: 12, TextColor: #9aa7b0); }");
            return;
        }

        for (Match match : matches) {
            String team1Names = getPlayerNames(match.team1());
            String team2Names = getPlayerNames(match.team2());
            String line = match.id() + " | " + match.type().name() + " | " + team1Names + " vs " + team2Names;
            String entry = "Label { Text: \"" + escapeUiText(line) + "\"; Style: (FontSize: 12, TextColor: #d7e0e8); Anchor: (Bottom: 6); }";
            commands.appendInline("#MatchesList", entry);
        }
    }

    private void bindActions(UIEventBuilder events) {
        bindNavigation(events);

        // Kit actions
        events.addEventBinding(CustomUIEventBindingType.Activating, "#KitCreateButton",
            EventData.of(AdminEventData.KEY_ACTION, "kit_create")
                .append(AdminEventData.KEY_KIT_NAME, "#NewKitIdInput.Value"));

        events.addEventBinding(CustomUIEventBindingType.Activating, "#KitDeleteButton",
            EventData.of(AdminEventData.KEY_ACTION, "kit_delete"));

        events.addEventBinding(CustomUIEventBindingType.Activating, "#KitSaveButton",
            EventData.of(AdminEventData.KEY_ACTION, "kit_save")
                .append(AdminEventData.KEY_KIT_NAME, "#KitNameInput.Value")
                .append(AdminEventData.KEY_KIT_ICON, "#KitIconInput.Value"));

        // Arena actions
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ArenaCreateButton",
            EventData.of(AdminEventData.KEY_ACTION, "arena_create")
                .append(AdminEventData.KEY_ARENA_NAME, "#NewArenaIdInput.Value"));

        events.addEventBinding(CustomUIEventBindingType.Activating, "#ArenaDeleteButton",
            EventData.of(AdminEventData.KEY_ACTION, "arena_delete"));

        events.addEventBinding(CustomUIEventBindingType.Activating, "#ArenaSetSpawn1Button",
            EventData.of(AdminEventData.KEY_ACTION, "arena_setspawn1"));

        events.addEventBinding(CustomUIEventBindingType.Activating, "#ArenaSetSpawn2Button",
            EventData.of(AdminEventData.KEY_ACTION, "arena_setspawn2"));

        // Note: Dynamic list selection removed - use commands instead
        // /kit save <name>, /kit delete <name>, etc.
    }

    private void bindNavigation(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavKitsButton",
            EventData.of(AdminEventData.KEY_ACTION, "tab_kits"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavArenasButton",
            EventData.of(AdminEventData.KEY_ACTION, "tab_arenas"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavMatchesButton",
            EventData.of(AdminEventData.KEY_ACTION, "tab_matches"));
    }

    // === Kit Handlers ===

    private void handleKitCreate(AdminEventData data) {
        String kitId = normalize(data.getKitName());
        if (kitId == null || kitId.isEmpty()) {
            sendNotice("ID du kit requis.");
            return;
        }
        kitId = kitId.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");

        if (plugin.core().kitService().kitExists(kitId)) {
            sendNotice("Ce kit existe deja.");
            return;
        }

        KitDefinition newKit = KitDefinition.builder(kitId)
            .displayName(kitId)
            .iconItem("hytale:iron_sword")
            .items(List.of())
            .build();

        plugin.core().kitService().createKit(newKit);
        selectedKit = kitId;
        sendNotice("Kit '" + kitId + "' cree.");
        plugin.flush();
    }

    private void handleKitSelect(AdminEventData data) {
        String kitId = normalize(data.getKitId());
        if (kitId != null && plugin.core().kitService().kitExists(kitId)) {
            selectedKit = kitId;
        }
    }

    private void handleKitDelete() {
        if (selectedKit == null) {
            sendNotice("Aucun kit selectionne.");
            return;
        }
        if (plugin.core().kitService().deleteKit(selectedKit)) {
            sendNotice("Kit '" + selectedKit + "' supprime.");
            selectedKit = null;
            plugin.flush();
        }
    }

    private void handleKitSave(AdminEventData data) {
        if (selectedKit == null) {
            sendNotice("Aucun kit selectionne.");
            return;
        }

        String newName = normalize(data.getKitName());
        String newIcon = normalize(data.getKitIcon());

        plugin.core().kitService().getKit(selectedKit).ifPresent(oldKit -> {
            KitDefinition updated = KitDefinition.builder(oldKit.id())
                .displayName(newName != null ? newName : oldKit.displayName())
                .iconItem(newIcon != null ? newIcon : oldKit.iconItem())
                .armor(oldKit.helmet(), oldKit.chestplate(), oldKit.leggings(), oldKit.boots())
                .items(oldKit.items() != null ? oldKit.items() : List.of())
                .effects(oldKit.effects() != null ? oldKit.effects() : java.util.Map.of())
                .build();

            plugin.core().kitService().createKit(updated);
            sendNotice("Kit sauvegarde.");
            plugin.flush();
        });
    }

    // === Arena Handlers ===

    private void handleArenaCreate(AdminEventData data) {
        String arenaId = normalize(data.getArenaName());
        if (arenaId == null || arenaId.isEmpty()) {
            sendNotice("ID de l'arene requis.");
            return;
        }
        arenaId = arenaId.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");

        if (plugin.core().arenaService().getArena(arenaId).isPresent()) {
            sendNotice("Cette arene existe deja.");
            return;
        }

        Arena newArena = Arena.builder(arenaId)
            .displayName(arenaId)
            .team1Spawns(List.of())
            .team2Spawns(List.of())
            .maxPlayers(2)
            .build();

        plugin.core().arenaService().addArena(newArena);
        sendNotice("Arene '" + arenaId + "' creee. Configure les spawns.");
        selectedArena = arenaId;
        plugin.flush();
    }

    private void handleArenaSelect(AdminEventData data) {
        String arenaId = normalize(data.getArenaId());
        if (arenaId != null && plugin.core().arenaService().getArena(arenaId).isPresent()) {
            selectedArena = arenaId;
        }
    }

    private void handleArenaDelete() {
        if (selectedArena == null) {
            sendNotice("Aucune arene selectionnee.");
            return;
        }
        if (plugin.core().arenaService().removeArena(selectedArena)) {
            sendNotice("Arene '" + selectedArena + "' supprimee.");
            selectedArena = null;
            plugin.flush();
        } else {
            sendNotice("Impossible de supprimer (arene en cours d'utilisation).");
        }
    }

    private void handleArenaSetSpawn(int team) {
        if (selectedArena == null) {
            sendNotice("Aucune arene selectionnee.");
            return;
        }
        // TODO: Get player position and add to arena spawns
        sendNotice("Spawn Team " + team + " defini a ta position actuelle.");
    }

    // === Match Handlers ===

    private void handleMatchEnd(AdminEventData data) {
        String matchId = normalize(data.getMatchId());
        if (matchId == null) {
            return;
        }
        plugin.core().matchService().endMatch(matchId, List.of());
        sendNotice("Match " + matchId + " termine.");
    }

    // === Helpers ===

    private void selectTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return;
        }
        switch (tab) {
            case "kits", "arenas", "matches" -> activeTab = tab;
        }
    }

    private List<Match> getActiveMatches() {
        return new ArrayList<>(plugin.core().matchService().getActiveMatches());
    }

    private long getActiveMatchCount() {
        return getActiveMatches().size();
    }

    private String getPlayerNames(List<java.util.UUID> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return "?";
        }
        return playerIds.stream()
            .map(id -> plugin.core().matchService().getPlayerMatch(id)
                .map(m -> "Joueur")
                .orElse("?"))
            .reduce((a, b) -> a + ", " + b)
            .orElse("?");
    }

    private void sendNotice(String message) {
        playerRef.sendMessage(Message.raw(PREFIX + message));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String escapeUiText(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // === Event Data Codec ===

    public static final class AdminEventData {
        static final String KEY_ACTION = "Action";
        static final String KEY_KIT_ID = "@KitId";
        static final String KEY_KIT_NAME = "@KitName";
        static final String KEY_KIT_ICON = "@KitIcon";
        static final String KEY_ARENA_ID = "@ArenaId";
        static final String KEY_ARENA_NAME = "@ArenaName";
        static final String KEY_MATCH_ID = "@MatchId";

        static final BuilderCodec<AdminEventData> CODEC = BuilderCodec.builder(AdminEventData.class, AdminEventData::new)
            .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), AdminEventData::setAction, AdminEventData::getAction).add()
            .append(new KeyedCodec<>(KEY_KIT_ID, Codec.STRING), AdminEventData::setKitId, AdminEventData::getKitId).add()
            .append(new KeyedCodec<>(KEY_KIT_NAME, Codec.STRING), AdminEventData::setKitName, AdminEventData::getKitName).add()
            .append(new KeyedCodec<>(KEY_KIT_ICON, Codec.STRING), AdminEventData::setKitIcon, AdminEventData::getKitIcon).add()
            .append(new KeyedCodec<>(KEY_ARENA_ID, Codec.STRING), AdminEventData::setArenaId, AdminEventData::getArenaId).add()
            .append(new KeyedCodec<>(KEY_ARENA_NAME, Codec.STRING), AdminEventData::setArenaName, AdminEventData::getArenaName).add()
            .append(new KeyedCodec<>(KEY_MATCH_ID, Codec.STRING), AdminEventData::setMatchId, AdminEventData::getMatchId).add()
            .build();

        private String action;
        private String kitId;
        private String kitName;
        private String kitIcon;
        private String arenaId;
        private String arenaName;
        private String matchId;

        public AdminEventData() {}

        public String getAction() { return action; }
        public String getKitId() { return kitId; }
        public String getKitName() { return kitName; }
        public String getKitIcon() { return kitIcon; }
        public String getArenaId() { return arenaId; }
        public String getArenaName() { return arenaName; }
        public String getMatchId() { return matchId; }

        private void setAction(String action) { this.action = action; }
        private void setKitId(String kitId) { this.kitId = kitId; }
        private void setKitName(String kitName) { this.kitName = kitName; }
        private void setKitIcon(String kitIcon) { this.kitIcon = kitIcon; }
        private void setArenaId(String arenaId) { this.arenaId = arenaId; }
        private void setArenaName(String arenaName) { this.arenaName = arenaName; }
        private void setMatchId(String matchId) { this.matchId = matchId; }
    }
}
