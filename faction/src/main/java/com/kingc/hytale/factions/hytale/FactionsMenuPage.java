package com.kingc.hytale.factions.hytale;

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
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kingc.hytale.factions.model.Faction;
import com.kingc.hytale.factions.model.MemberCombatStats;
import com.kingc.hytale.factions.model.MemberRole;
import com.kingc.hytale.factions.model.NotificationEntry;
import com.kingc.hytale.factions.model.War;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class FactionsMenuPage extends InteractiveCustomUIPage<FactionsMenuPage.FactionsMenuEventData> {
    private static final String PAGE_UI = "Pages/FactionsMenu.ui";
    private static final String PREFIX = "[Factions] ";

    private final HytaleFactionsPlugin plugin;
    private String activeTab = "manage";

    public FactionsMenuPage(HytaleFactionsPlugin plugin, PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, FactionsMenuEventData.CODEC);
        this.plugin = plugin;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events, Store<EntityStore> store) {
        commands.append(PAGE_UI);
        updateStatus(commands);
        buildFactionList(commands);
        buildMembersList(commands);
        buildAlliesList(commands);
        buildClaimInfo(commands);
        buildCombatStats(commands);
        buildWarStatus(commands);
        buildLeaderboard(commands);
        buildEventHistory(commands);
        buildPerks(commands);
        applyTabState(commands);
        bindActions(events);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, FactionsMenuEventData data) {
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
            refresh();
            return;
        }
        if (action.equals("close")) {
            close();
            return;
        }

        String command = switch (action) {
            case "create" -> buildCommand("create", requireFactionName(data));
            case "rename" -> buildCommand("rename", requireFactionName(data));
            case "desc" -> buildDescriptionCommand(data);
            case "info" -> buildOptionalCommand("info", normalize(data.getFactionName()));
            case "who" -> buildCommand("who", requirePlayerName(data));
            case "map" -> "f map";
            case "borders" -> buildBordersCommand(data);
            case "invite" -> buildCommand("invite", requirePlayerName(data));
            case "accept" -> buildCommand("accept", requireFactionName(data));
            case "deny" -> buildCommand("deny", requireFactionName(data));
            case "disband" -> "f disband";
            case "leave" -> "f leave";
            case "kick" -> buildCommand("kick", requirePlayerName(data));
            case "promote" -> buildCommand("promote", requirePlayerName(data));
            case "demote" -> buildCommand("demote", requirePlayerName(data));
            case "leader" -> buildCommand("leader", requirePlayerName(data));
            case "ally" -> buildCommand("ally", requireFactionName(data));
            case "unally" -> buildCommand("unally", requireFactionName(data));
            case "sethome" -> "f sethome";
            case "home" -> "f home";
            case "claim" -> "f claim";
            case "unclaim" -> "f unclaim";
            case "stats" -> buildOptionalCommand("stats", normalize(data.getPlayerName()));
            case "top_kills" -> "f top kills";
            case "top_kdr" -> "f top kdr";
            case "top_factions" -> "f top factions";
            case "war_declare" -> buildWarDeclareCommand(requireFactionName(data));
            case "war_status" -> "f war status";
            case "war_surrender" -> "f war surrender";
            case "list" -> "f list";
            default -> null;
        };

        if (command == null) {
            return;
        }

        plugin.core().onCommand(new HytaleUiCommandSource(playerRef), command);
        plugin.flush();
        refresh();
    }

    private void refresh() {
        UICommandBuilder commands = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        updateStatus(commands);
        buildFactionList(commands);
        buildMembersList(commands);
        buildAlliesList(commands);
        buildClaimInfo(commands);
        buildCombatStats(commands);
        buildWarStatus(commands);
        buildLeaderboard(commands);
        buildEventHistory(commands);
        buildPerks(commands);
        applyTabState(commands);
        bindActions(events);
        sendUpdate(commands, events, false);
    }

    private void updateStatus(UICommandBuilder commands) {
        UUID playerId = playerRef.getUuid();
        String status;
        String details;
        String description;
        if (playerId == null) {
            status = "Statut inconnu.";
            details = "";
            description = "";
        } else {
            Optional<Faction> faction = plugin.core().service().findFactionByMember(playerId);
            if (faction.isEmpty()) {
                status = "Aucune faction.";
                details = "Utilise les champs ci-dessous.";
                description = "Aucune description.";
            } else {
                Faction current = faction.get();
                MemberRole role = current.roleOf(playerId);
                int claims = plugin.core().service().getClaimCount(current.id());
                int claimLimit = plugin.core().service().getClaimLimit(current.id());
                int power = plugin.core().service().getPower(current.id());
                int allies = current.allies().size();
                int members = current.members().size();
                String roleLabel = formatRole(role);
                status = "Faction: " + current.name() + " (" + roleLabel + ")";
                details = "Membres: " + members + " | Claims: " + claims + "/" + claimLimit + " | Allies: " + allies + " | Power: " + power;
                description = current.description() == null || current.description().isBlank()
                        ? "Aucune description."
                        : current.description();
            }
        }
        commands.set("#FactionStatus.TextSpans", Message.raw(status));
        commands.set("#FactionDetails.TextSpans", Message.raw(details));
        commands.set("#FactionDescription.TextSpans", Message.raw(description));
    }

    private void buildFactionList(UICommandBuilder commands) {
        commands.clear("#FactionsList");
        List<Faction> factions = new ArrayList<>(plugin.core().service().getAllFactions());
        factions.sort(Comparator.comparing(faction -> faction.name().toLowerCase(Locale.ROOT)));
        if (factions.isEmpty()) {
            commands.appendInline("#FactionsList",
                    "Label { Text: \"Aucune faction\"; Style: (Alignment: Center, FontSize: 12, TextColor: #9aa7b0); }");
            return;
        }
        for (Faction faction : factions) {
            int members = faction.members().size();
            int claims = plugin.core().service().getClaimCount(faction.id());
            String line = faction.name() + " - " + members + " membres, " + claims + " claims";
            String entry = "Label { Text: \"" + escapeUiText(line) + "\"; Style: (FontSize: 13, TextColor: #d7e0e8); Anchor: (Bottom: 6); }";
            commands.appendInline("#FactionsList", entry);
        }
    }

    private void buildMembersList(UICommandBuilder commands) {
        commands.clear("#MembersList");
        Optional<Faction> faction = getCurrentFaction();
        if (faction.isEmpty()) {
            commands.appendInline("#MembersList",
                    "Label { Text: \"Aucun membre\"; Style: (Alignment: Center, FontSize: 12, TextColor: #9aa7b0); }");
            return;
        }
        List<Map.Entry<UUID, MemberRole>> members = new ArrayList<>(faction.get().members().entrySet());
        members.sort(Comparator.comparing((Map.Entry<UUID, MemberRole> entry) -> entry.getValue().rank()).reversed()
                .thenComparing(entry -> resolveName(entry.getKey()).toLowerCase(Locale.ROOT)));
        for (Map.Entry<UUID, MemberRole> entry : members) {
            String line = formatRole(entry.getValue()) + " - " + resolveName(entry.getKey());
            String item = "Label { Text: \"" + escapeUiText(line) + "\"; Style: (FontSize: 12, TextColor: #c6d3dd); Anchor: (Bottom: 6); }";
            commands.appendInline("#MembersList", item);
        }
    }

    private void buildAlliesList(UICommandBuilder commands) {
        commands.clear("#AlliesList");
        Optional<Faction> faction = getCurrentFaction();
        if (faction.isEmpty() || faction.get().allies().isEmpty()) {
            commands.appendInline("#AlliesList",
                    "Label { Text: \"Aucune alliance\"; Style: (Alignment: Center, FontSize: 12, TextColor: #9aa7b0); }");
            return;
        }
        List<UUID> allies = new ArrayList<>(faction.get().allies());
        allies.sort(Comparator.comparing(id -> plugin.core().service().getFactionById(id).map(Faction::name).orElse(id.toString())));
        for (UUID allyId : allies) {
            String name = plugin.core().service().getFactionById(allyId).map(Faction::name).orElse(shortId(allyId));
            String item = "Label { Text: \"" + escapeUiText(name) + "\"; Style: (FontSize: 12, TextColor: #c6d3dd); Anchor: (Bottom: 6); }";
            commands.appendInline("#AlliesList", item);
        }
    }

    private void buildClaimInfo(UICommandBuilder commands) {
        Optional<Faction> faction = getCurrentFaction();
        if (faction.isEmpty()) {
            commands.set("#ClaimStats.TextSpans", Message.raw("Aucune faction."));
            return;
        }
        Faction current = faction.get();
        int claims = plugin.core().service().getClaimCount(current.id());
        int claimLimit = plugin.core().service().getClaimLimit(current.id());
        int power = plugin.core().service().getPower(current.id());
        commands.set("#ClaimStats.TextSpans", Message.raw("Claims: " + claims + "/" + claimLimit + " | Power: " + power));
    }

    private void buildCombatStats(UICommandBuilder commands) {
        commands.clear("#CombatStatsList");
        UUID playerId = playerRef.getUuid();
        if (playerId == null) {
            appendListLabel(commands, "#CombatStatsList", "Aucun joueur.", "#9aa7b0");
            return;
        }
        MemberCombatStats stats = plugin.core().combatService().getMemberStats(playerId);
        appendListLabel(commands, "#CombatStatsList", "Vos stats", "#a9b7c4");
        appendListLabel(commands, "#CombatStatsList", "Kills: " + stats.kills() + " | Deaths: " + stats.deaths() + " | K/D: " + stats.kdr(), "#c6d3dd");
        appendListLabel(commands, "#CombatStatsList", "Streak: " + stats.currentStreak() + " (best " + stats.bestStreak() + ")", "#c6d3dd");
        appendListLabel(commands, "#CombatStatsList", "Top killers", "#a9b7c4");
        List<MemberCombatStats> topKillers = plugin.core().combatService().getTopKillers(5);
        if (topKillers.isEmpty()) {
            appendListLabel(commands, "#CombatStatsList", "Aucune donnee.", "#9aa7b0");
        } else {
            int rank = 1;
            for (MemberCombatStats entry : topKillers) {
                String name = resolveName(entry.playerId());
                appendListLabel(commands, "#CombatStatsList",
                        rank + ". " + name + " - " + entry.kills() + " kills (K/D " + entry.kdr() + ")",
                        "#c6d3dd");
                rank++;
            }
        }
    }

    private void buildWarStatus(UICommandBuilder commands) {
        commands.clear("#WarStatusList");
        Optional<Faction> faction = getCurrentFaction();
        if (faction.isEmpty()) {
            appendListLabel(commands, "#WarStatusList", "Aucune faction.", "#9aa7b0");
            return;
        }
        Optional<War> warOpt = plugin.core().combatService().getActiveWar(faction.get().id());
        if (warOpt.isEmpty()) {
            appendListLabel(commands, "#WarStatusList", "Pas de guerre active.", "#9aa7b0");
            return;
        }
        War war = warOpt.get();
        String attacker = plugin.core().service().getFactionById(war.attackerFactionId()).map(Faction::name).orElse("Attaquant");
        String defender = plugin.core().service().getFactionById(war.defenderFactionId()).map(Faction::name).orElse("Defenseur");
        appendListLabel(commands, "#WarStatusList", attacker + " vs " + defender, "#c6d3dd");
        appendListLabel(commands, "#WarStatusList", "Etat: " + war.state().name().toLowerCase(Locale.ROOT), "#c6d3dd");
        appendListLabel(commands, "#WarStatusList", "Points: " + war.attackerPoints() + " - " + war.defenderPoints(), "#c6d3dd");
        appendListLabel(commands, "#WarStatusList", "Kills: " + war.attackerKills() + " - " + war.defenderKills(), "#c6d3dd");
    }

    private void buildLeaderboard(UICommandBuilder commands) {
        commands.clear("#LeaderboardList");
        List<com.kingc.hytale.factions.model.FactionCombatStats> top = plugin.core().combatService().getTopFactions(8);
        if (top.isEmpty()) {
            appendListLabel(commands, "#LeaderboardList", "Aucun classement.", "#9aa7b0");
            return;
        }
        int rank = 1;
        for (com.kingc.hytale.factions.model.FactionCombatStats stats : top) {
            String name = plugin.core().service().getFactionById(stats.factionId()).map(Faction::name).orElse("Unknown");
            appendListLabel(commands, "#LeaderboardList", rank + ". " + name + " - " + stats.totalKills() + " kills", "#c6d3dd");
            rank++;
        }
    }

    private void buildEventHistory(UICommandBuilder commands) {
        commands.clear("#EventsList");
        UUID playerId = playerRef.getUuid();
        if (playerId == null) {
            appendListLabel(commands, "#EventsList", "Aucun joueur.", "#9aa7b0");
            return;
        }
        List<NotificationEntry> history = plugin.core().service().getNotificationHistory(playerId, null, 12);
        if (history.isEmpty()) {
            appendListLabel(commands, "#EventsList", "Aucun evenement.", "#9aa7b0");
            return;
        }
        for (NotificationEntry entry : history) {
            String line = "[" + entry.type().name().toLowerCase(Locale.ROOT) + "] " + entry.title() + " - " + entry.message();
            appendListLabel(commands, "#EventsList", line, "#c6d3dd");
        }
    }

    private void buildPerks(UICommandBuilder commands) {
        commands.clear("#PerksList");
        appendListLabel(commands, "#PerksList", "Perks indisponibles pour le moment.", "#9aa7b0");
    }

    private void bindActions(UIEventBuilder events) {
        bindNavigation(events);
        bindClose(events);
        addAction(events, "#CreateButton", "create", true, false);
        addAction(events, "#RenameButton", "rename", true, false);
        addActionWithDescription(events, "#DescButton", "desc");
        addActionOptionalFaction(events, "#InfoButton", "info");
        addAction(events, "#InviteButton", "invite", false, true);
        addAction(events, "#AcceptButton", "accept", true, false);
        addAction(events, "#DenyButton", "deny", true, false);
        addAction(events, "#AllyButton", "ally", true, false);
        addAction(events, "#UnallyButton", "unally", true, false);
        addAction(events, "#ListButton", "list", false, false);
        addAction(events, "#DisbandButton", "disband", false, false);
        addAction(events, "#LeaveButton", "leave", false, false);
        addAction(events, "#KickButton", "kick", false, true);
        addAction(events, "#PromoteButton", "promote", false, true);
        addAction(events, "#DemoteButton", "demote", false, true);
        addAction(events, "#LeaderButton", "leader", false, true);
        addAction(events, "#ClaimButton", "claim", false, false);
        addAction(events, "#UnclaimButton", "unclaim", false, false);
        addAction(events, "#SetHomeButton", "sethome", false, false);
        addAction(events, "#HomeButton", "home", false, false);
        addAction(events, "#MapButton", "map", false, false);
        addActionWithBordersSeconds(events, "#BordersButton", "borders");
        addAction(events, "#WhoButton", "who", false, true);
        addAction(events, "#StatsButton", "stats", false, true);
        addAction(events, "#TopKillsButton", "top_kills", false, false);
        addAction(events, "#TopKdrButton", "top_kdr", false, false);
        addAction(events, "#TopFactionsButton", "top_factions", false, false);
        addAction(events, "#WarDeclareButton", "war_declare", true, false);
        addAction(events, "#WarStatusButton", "war_status", false, false);
        addAction(events, "#WarSurrenderButton", "war_surrender", false, false);
    }

    private void bindClose(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of(FactionsMenuEventData.KEY_ACTION, "close"));
    }

    private void bindNavigation(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavManageButton",
                EventData.of(FactionsMenuEventData.KEY_ACTION, "tab_manage"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavMembersButton",
                EventData.of(FactionsMenuEventData.KEY_ACTION, "tab_members"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavRelationsButton",
                EventData.of(FactionsMenuEventData.KEY_ACTION, "tab_relations"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavClaimsButton",
                EventData.of(FactionsMenuEventData.KEY_ACTION, "tab_claims"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavToolsButton",
                EventData.of(FactionsMenuEventData.KEY_ACTION, "tab_tools"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavCombatButton",
                EventData.of(FactionsMenuEventData.KEY_ACTION, "tab_combat"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavWarButton",
                EventData.of(FactionsMenuEventData.KEY_ACTION, "tab_war"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavLeaderboardButton",
                EventData.of(FactionsMenuEventData.KEY_ACTION, "tab_leaderboard"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavEventsButton",
                EventData.of(FactionsMenuEventData.KEY_ACTION, "tab_events"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NavPerksButton",
                EventData.of(FactionsMenuEventData.KEY_ACTION, "tab_perks"));
    }

    private void applyTabState(UICommandBuilder commands) {
        boolean manage = "manage".equals(activeTab);
        boolean members = "members".equals(activeTab);
        boolean relations = "relations".equals(activeTab);
        boolean claims = "claims".equals(activeTab);
        boolean tools = "tools".equals(activeTab);
        boolean combat = "combat".equals(activeTab);
        boolean war = "war".equals(activeTab);
        boolean leaderboard = "leaderboard".equals(activeTab);
        boolean events = "events".equals(activeTab);
        boolean perks = "perks".equals(activeTab);

        commands.set("#ManageView.Visible", manage);
        commands.set("#MembersView.Visible", members);
        commands.set("#RelationsView.Visible", relations);
        commands.set("#ClaimsView.Visible", claims);
        commands.set("#ToolsView.Visible", tools);
        commands.set("#CombatView.Visible", combat);
        commands.set("#WarView.Visible", war);
        commands.set("#LeaderboardView.Visible", leaderboard);
        commands.set("#EventsView.Visible", events);
        commands.set("#PerksView.Visible", perks);

        commands.set("#FactionInputPanel.Visible", manage || relations || tools || war);
        commands.set("#PlayerInputPanel.Visible", members || tools || combat);

        commands.set("#NavManageIndicator.Visible", manage);
        commands.set("#NavMembersIndicator.Visible", members);
        commands.set("#NavRelationsIndicator.Visible", relations);
        commands.set("#NavClaimsIndicator.Visible", claims);
        commands.set("#NavToolsIndicator.Visible", tools);
        commands.set("#NavCombatIndicator.Visible", combat);
        commands.set("#NavWarIndicator.Visible", war);
        commands.set("#NavLeaderboardIndicator.Visible", leaderboard);
        commands.set("#NavEventsIndicator.Visible", events);
        commands.set("#NavPerksIndicator.Visible", perks);
    }

    private void selectTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return;
        }
        switch (tab) {
            case "manage", "members", "relations", "claims", "tools", "combat", "war", "leaderboard", "events", "perks" -> activeTab = tab;
            default -> {
            }
        }
    }

    private void addAction(UIEventBuilder events, String elementId, String action, boolean needsFaction, boolean needsPlayer) {
        EventData data = EventData.of(FactionsMenuEventData.KEY_ACTION, action);
        if (needsFaction) {
            data = data.append(FactionsMenuEventData.KEY_FACTION_NAME, "#FactionNameInput.Value");
        }
        if (needsPlayer) {
            data = data.append(FactionsMenuEventData.KEY_PLAYER_NAME, "#PlayerNameInput.Value");
        }
        events.addEventBinding(CustomUIEventBindingType.Activating, elementId, data);
    }

    private void addActionOptionalFaction(UIEventBuilder events, String elementId, String action) {
        EventData data = EventData.of(FactionsMenuEventData.KEY_ACTION, action)
                .append(FactionsMenuEventData.KEY_FACTION_NAME, "#FactionNameInput.Value");
        events.addEventBinding(CustomUIEventBindingType.Activating, elementId, data);
    }

    private void addActionWithDescription(UIEventBuilder events, String elementId, String action) {
        EventData data = EventData.of(FactionsMenuEventData.KEY_ACTION, action)
                .append(FactionsMenuEventData.KEY_DESCRIPTION, "#DescriptionInput.Value");
        events.addEventBinding(CustomUIEventBindingType.Activating, elementId, data);
    }

    private void addActionWithBordersSeconds(UIEventBuilder events, String elementId, String action) {
        EventData data = EventData.of(FactionsMenuEventData.KEY_ACTION, action)
                .append(FactionsMenuEventData.KEY_BORDERS_SECONDS, "#BordersSecondsInput.Value");
        events.addEventBinding(CustomUIEventBindingType.Activating, elementId, data);
    }

    private String requireFactionName(FactionsMenuEventData data) {
        String name = normalize(data.getFactionName());
        if (name == null) {
            sendNotice("Nom de faction requis.");
            return null;
        }
        return name;
    }

    private String requirePlayerName(FactionsMenuEventData data) {
        String name = normalize(data.getPlayerName());
        if (name == null) {
            sendNotice("Nom de joueur requis.");
            return null;
        }
        return name;
    }

    private String buildCommand(String action, String value) {
        if (value == null) {
            return null;
        }
        return "f " + action + " " + value;
    }

    private String buildOptionalCommand(String action, String value) {
        if (value == null) {
            return "f " + action;
        }
        return "f " + action + " " + value;
    }

    private String buildWarDeclareCommand(String factionName) {
        if (factionName == null) {
            return null;
        }
        return "f war declare " + factionName;
    }

    private String buildDescriptionCommand(FactionsMenuEventData data) {
        String description = normalize(data.getDescription());
        if (description == null) {
            return "f desc";
        }
        return "f desc " + description;
    }

    private String buildBordersCommand(FactionsMenuEventData data) {
        String raw = normalize(data.getBordersSeconds());
        if (raw == null) {
            return "f borders 30";
        }
        try {
            int seconds = Integer.parseInt(raw);
            if (seconds < 0) {
                seconds = 0;
            } else if (seconds > 120) {
                seconds = 120;
            }
            return "f borders " + seconds;
        } catch (NumberFormatException e) {
            return "f borders";
        }
    }

    private void appendListLabel(UICommandBuilder commands, String listId, String text, String color) {
        String safeText = escapeUiText(text == null ? "" : text);
        String safeColor = color == null || color.isBlank() ? "#c6d3dd" : color;
        String entry = "Label { Text: \"" + safeText + "\"; Style: (FontSize: 12, TextColor: " + safeColor + "); Anchor: (Bottom: 6); }";
        commands.appendInline(listId, entry);
    }

    private void sendNotice(String message) {
        playerRef.sendMessage(Message.raw(PREFIX + message));
    }

    private Optional<Faction> getCurrentFaction() {
        UUID playerId = playerRef.getUuid();
        if (playerId == null) {
            return Optional.empty();
        }
        return plugin.core().service().findFactionByMember(playerId);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveName(UUID id) {
        if (id == null) {
            return "unknown";
        }
        PlayerRef ref = Universe.get().getPlayer(id);
        if (ref != null && ref.getUsername() != null && !ref.getUsername().isBlank()) {
            return ref.getUsername();
        }
        return shortId(id);
    }

    private String formatRole(MemberRole role) {
        if (role == null) {
            return "membre";
        }
        return switch (role) {
            case LEADER -> "chef";
            case OFFICER -> "officier";
            case MEMBER -> "membre";
            case RECRUIT -> "recrue";
        };
    }

    private String shortId(UUID id) {
        String raw = id.toString();
        return raw.substring(0, Math.min(raw.length(), 8));
    }

    private String escapeUiText(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static final class FactionsMenuEventData {
        static final String KEY_ACTION = "Action";
        static final String KEY_FACTION_NAME = "@FactionName";
        static final String KEY_PLAYER_NAME = "@PlayerName";
        static final String KEY_DESCRIPTION = "@Description";
        static final String KEY_BORDERS_SECONDS = "@BordersSeconds";

        static final BuilderCodec<FactionsMenuEventData> CODEC = BuilderCodec.builder(FactionsMenuEventData.class, FactionsMenuEventData::new)
                .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), FactionsMenuEventData::setAction, FactionsMenuEventData::getAction)
                .add()
                .append(new KeyedCodec<>(KEY_FACTION_NAME, Codec.STRING), FactionsMenuEventData::setFactionName, FactionsMenuEventData::getFactionName)
                .add()
                .append(new KeyedCodec<>(KEY_PLAYER_NAME, Codec.STRING), FactionsMenuEventData::setPlayerName, FactionsMenuEventData::getPlayerName)
                .add()
                .append(new KeyedCodec<>(KEY_DESCRIPTION, Codec.STRING), FactionsMenuEventData::setDescription, FactionsMenuEventData::getDescription)
                .add()
                .append(new KeyedCodec<>(KEY_BORDERS_SECONDS, Codec.STRING), FactionsMenuEventData::setBordersSeconds, FactionsMenuEventData::getBordersSeconds)
                .add()
                .build();

        private String action;
        private String factionName;
        private String playerName;
        private String description;
        private String bordersSeconds;

        public FactionsMenuEventData() {
        }

        public String getAction() {
            return action;
        }

        public String getFactionName() {
            return factionName;
        }

        public String getPlayerName() {
            return playerName;
        }

        public String getDescription() {
            return description;
        }

        public String getBordersSeconds() {
            return bordersSeconds;
        }

        private void setAction(String action) {
            this.action = action;
        }

        private void setFactionName(String factionName) {
            this.factionName = factionName;
        }

        private void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

        private void setDescription(String description) {
            this.description = description;
        }

        private void setBordersSeconds(String bordersSeconds) {
            this.bordersSeconds = bordersSeconds;
        }
    }
}
