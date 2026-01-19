package com.kingc.hytale.factions.command;

import com.kingc.hytale.factions.api.ClaimChangeType;
import com.kingc.hytale.factions.api.ClaimEffectHandler;
import com.kingc.hytale.factions.api.CommandSource;
import com.kingc.hytale.factions.api.FactionCreateHandler;
import com.kingc.hytale.factions.api.Location;
import com.kingc.hytale.factions.api.MemberRoleChangeHandler;
import com.kingc.hytale.factions.api.ServerAdapter;
import com.kingc.hytale.factions.api.WarDeclareHandler;
import com.kingc.hytale.factions.model.ClaimKey;
import com.kingc.hytale.factions.model.Faction;
import com.kingc.hytale.factions.model.FactionCombatStats;
import com.kingc.hytale.factions.model.MemberCombatStats;
import com.kingc.hytale.factions.model.MemberRole;
import com.kingc.hytale.factions.model.NotificationEntry;
import com.kingc.hytale.factions.model.NotificationType;
import com.kingc.hytale.factions.model.War;
import com.kingc.hytale.factions.service.CombatService;
import com.kingc.hytale.factions.service.FactionService;
import com.kingc.hytale.factions.service.FactionSettings;
import com.kingc.hytale.factions.service.Result;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class CommandDispatcher {
    private static final String PREFIX = "[Factions] ";

    private final FactionService service;
    private final CombatService combatService;
    private final ServerAdapter server;
    private final FactionSettings settings;
    private final Supplier<Result<Void>> reloadHandler;
    private java.util.function.Function<UUID, String> chatToggleHandler;
    private BiFunction<UUID, Integer, String> borderToggleHandler;
    private java.util.function.Function<UUID, String> worldMapToggleHandler;
    private ClaimEffectHandler claimEffectHandler;
    private FactionCreateHandler factionCreateHandler;
    private WarDeclareHandler warDeclareHandler;
    private MemberRoleChangeHandler memberRoleChangeHandler;

    public CommandDispatcher(FactionService service, ServerAdapter server, FactionSettings settings,
            Supplier<Result<Void>> reloadHandler) {
        this(service, null, server, settings, reloadHandler, null);
    }

    public CommandDispatcher(FactionService service, CombatService combatService, ServerAdapter server,
            FactionSettings settings, Supplier<Result<Void>> reloadHandler) {
        this(service, combatService, server, settings, reloadHandler, null);
    }

    public CommandDispatcher(FactionService service, CombatService combatService, ServerAdapter server,
            FactionSettings settings, Supplier<Result<Void>> reloadHandler,
            java.util.function.Function<UUID, String> chatToggleHandler) {
        this.service = service;
        this.combatService = combatService;
        this.server = server;
        this.settings = settings;
        this.reloadHandler = reloadHandler;
        this.chatToggleHandler = chatToggleHandler;
    }

    public void setChatToggleHandler(java.util.function.Function<UUID, String> handler) {
        this.chatToggleHandler = handler;
    }

    public void setBorderToggleHandler(BiFunction<UUID, Integer, String> handler) {
        this.borderToggleHandler = handler;
    }

    public void setWorldMapToggleHandler(java.util.function.Function<UUID, String> handler) {
        this.worldMapToggleHandler = handler;
    }

    public void setClaimEffectHandler(ClaimEffectHandler handler) {
        this.claimEffectHandler = handler;
    }

    public void setFactionCreateHandler(FactionCreateHandler handler) {
        this.factionCreateHandler = handler;
    }

    public void setWarDeclareHandler(WarDeclareHandler handler) {
        this.warDeclareHandler = handler;
    }

    public void setMemberRoleChangeHandler(MemberRoleChangeHandler handler) {
        this.memberRoleChangeHandler = handler;
    }

    public boolean handle(CommandSource source, String commandLine) {
        String trimmed = commandLine == null ? "" : commandLine.trim();
        if (trimmed.isEmpty()) {
            sendHelp(source);
            return true;
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 0) {
            sendHelp(source);
            return true;
        }
        if (parts[0].equalsIgnoreCase("f") || parts[0].equalsIgnoreCase("faction")) {
            parts = Arrays.copyOfRange(parts, 1, parts.length);
            if (parts.length == 0) {
                sendHelp(source);
                return true;
            }
        }

        String sub = parts[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> sendHelp(source);
            case "create" -> handleCreate(source, parts);
            case "reload" -> handleReload(source);
            case "disband" -> handleDisband(source);
            case "list" -> handleList(source);
            case "info" -> handleInfo(source, parts);
            case "who" -> handleWho(source, parts);
            case "map" -> handleMap(source);
            case "invites" -> handleInvites(source);
            case "notify" -> handleNotify(source, parts);
            case "admin" -> handleAdmin(source, parts);
            case "rename" -> handleRename(source, parts);
            case "desc" -> handleDesc(source, parts);
            case "invite" -> handleInvite(source, parts);
            case "accept" -> handleAccept(source, parts);
            case "deny" -> handleDeny(source, parts);
            case "leave" -> handleLeave(source);
            case "kick" -> handleKick(source, parts);
            case "promote" -> handlePromote(source, parts);
            case "demote" -> handleDemote(source, parts);
            case "leader" -> handleLeader(source, parts);
            case "ally" -> handleAlly(source, parts);
            case "unally" -> handleUnally(source, parts);
            case "enemy" -> handleEnemy(source, parts);
            case "unenemy" -> handleUnenemy(source, parts);
            case "sethome" -> handleSetHome(source);
            case "home" -> handleHome(source);
            case "claim" -> handleClaim(source);
            case "unclaim" -> handleUnclaim(source);
            case "chat" -> handleChatToggle(source);
            case "borders" -> handleBorders(source, parts);
            case "stats" -> handleStats(source, parts);
            case "top" -> handleTop(source, parts);
            case "war" -> handleWar(source, parts);
            default -> {
                send(source, "Unknown subcommand. Use /f help.");
                return false;
            }
        }
        return true;
    }

    private void handleCreate(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 2) {
            send(source, "Usage: /f create <name>");
            return;
        }
        Result<Faction> result = service.createFaction(playerId, parts[1]);
        send(source, result.message());
        if (result.ok() && factionCreateHandler != null) {
            Optional<Location> location = server.getPlayerLocation(playerId);
            if (location.isPresent() && result.value() != null) {
                Faction faction = result.value();
                factionCreateHandler.handle(playerId, location.get(), faction.id(), faction.name());
            }
        }
    }

    private void handleRename(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 2) {
            send(source, "Usage: /f rename <name>");
            return;
        }
        Result<Void> result = service.rename(playerId, parts[1]);
        send(source, result.message());
    }

    private void handleDesc(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 2) {
            Optional<Faction> faction = service.findFactionByMember(playerId);
            if (faction.isEmpty()) {
                send(source, "You are not in a faction.");
                return;
            }
            String description = faction.get().description();
            if (description == null || description.isBlank()) {
                send(source, "Description: none");
            } else {
                send(source, "Description: " + description);
            }
            return;
        }
        String description = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
        Result<Void> result = service.setDescription(playerId, description);
        send(source, result.message());
    }

    private void handleList(CommandSource source) {
        var factions = service.getAllFactions();
        if (factions.isEmpty()) {
            send(source, "No factions created yet.");
            return;
        }
        send(source, "Factions (" + factions.size() + "):");
        for (Faction faction : factions) {
            int members = faction.members().size();
            int claims = service.getClaimCount(faction.id());
            send(source, faction.name() + " - " + members + " members, " + claims + " claims");
        }
    }

    private void handleInfo(CommandSource source, String[] parts) {
        Optional<Faction> factionOpt;
        if (parts.length >= 2) {
            factionOpt = service.findFactionByName(parts[1]);
        } else {
            UUID playerId = requirePlayer(source);
            if (playerId == null) {
                return;
            }
            factionOpt = service.findFactionByMember(playerId);
        }
        if (factionOpt.isEmpty()) {
            send(source, "Faction not found.");
            return;
        }
        Faction faction = factionOpt.get();
        UUID leaderId = findLeaderId(faction);
        String leaderName = leaderId == null ? "Unknown" : resolveName(leaderId);
        int members = faction.members().size();
        int claims = service.getClaimCount(faction.id());
        int claimLimit = service.getClaimLimit(faction.id());
        int power = service.getPower(faction.id());

        send(source, "Faction: " + faction.name());
        send(source, "Leader: " + leaderName + " | Members: " + members + "/" + settings.maxMembers);
        send(source, "Claims: " + claims + "/" + claimLimit + " | Power: " + power + " | Allies: "
                + faction.allies().size());
        String description = faction.description();
        if (description == null || description.isBlank()) {
            send(source, "Description: none");
        } else {
            send(source, "Description: " + description);
        }
    }

    private void handleWho(CommandSource source, String[] parts) {
        if (parts.length < 2) {
            send(source, "Usage: /f who <player>");
            return;
        }
        Optional<UUID> targetId = server.resolvePlayerId(parts[1]);
        if (targetId.isEmpty()) {
            send(source, "Player not found.");
            return;
        }
        UUID id = targetId.get();
        String targetName = resolveName(id);
        Optional<Faction> factionOpt = service.findFactionByMember(id);
        if (factionOpt.isEmpty()) {
            send(source, targetName + " is not in a faction.");
            return;
        }
        Faction faction = factionOpt.get();
        MemberRole role = faction.roleOf(id);
        send(source, targetName + " -> " + faction.name() + " (" + formatRole(role) + ")");
    }

    private void handleMap(CommandSource source) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        Optional<Location> location = server.getPlayerLocation(playerId);
        if (location.isEmpty()) {
            send(source, "Location unavailable.");
            return;
        }
        int radius = Math.min(Math.max(settings.mapRadius, 1), 10);
        FactionService.ClaimMap map = service.buildClaimMap(location.get(), playerId, radius);
        send(source, "Map: " + map.world() + " (chunk " + map.centerX() + ", " + map.centerZ() + "), radius " + radius);
        for (String line : map.lines()) {
            send(source, line);
        }
        send(source, map.legend());

        if (worldMapToggleHandler != null) {
            String message = worldMapToggleHandler.apply(playerId);
            if (message != null && !message.isBlank()) {
                send(source, message);
            }
        }
    }

    private void handleInvites(CommandSource source) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        var invites = service.getInvites(playerId);
        if (invites.isEmpty()) {
            send(source, "No invites pending.");
            return;
        }
        long now = server.nowEpochMs();
        send(source, "Invites (" + invites.size() + "):");
        for (var invite : invites) {
            String factionName = service.getFactionById(invite.factionId()).map(Faction::name).orElse("Unknown");
            String inviterName = resolveName(invite.inviterId());
            long remainingSeconds = Math.max(0L, (invite.expiresAtEpochMs() - now) / 1000L);
            send(source, factionName + " (by " + inviterName + ", " + remainingSeconds + "s left)");
        }
    }

    private void handleNotify(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 2) {
            boolean enabled = service.isNotificationsEnabled(playerId);
            send(source, "Notifications: " + (enabled ? "on" : "off"));
            return;
        }
        String mode = parts[1].toLowerCase(Locale.ROOT);
        if (mode.equals("history") || mode.equals("list")) {
            NotificationType type = null;
            Integer limit = null;
            if (parts.length >= 3) {
                type = NotificationType.fromString(parts[2]);
                if (type == null) {
                    Integer parsed = parseInt(parts[2]);
                    if (parsed == null) {
                        send(source, "Unknown type. Use: minor, major, war, territory, role, system.");
                        return;
                    }
                    limit = parsed;
                }
            }
            if (parts.length >= 4) {
                limit = parseInt(parts[3]);
                if (limit == null) {
                    send(source, "Usage: /f notify history [type] [limit]");
                    return;
                }
            }
            int max = limit != null ? limit : settings.notificationHistoryLimit;
            List<NotificationEntry> history = service.getNotificationHistory(playerId, type, max);
            if (history.isEmpty()) {
                send(source, "No notifications.");
                return;
            }
            send(source, "Notifications (" + history.size() + "):");
            for (NotificationEntry entry : history) {
                send(source, "[" + entry.type().name().toLowerCase(Locale.ROOT) + "] " + entry.title() + " - "
                        + entry.message());
            }
            return;
        }
        if (mode.equals("on")) {
            service.setNotificationsEnabled(playerId, true);
            send(source, "Notifications enabled.");
            return;
        }
        if (mode.equals("off")) {
            service.setNotificationsEnabled(playerId, false);
            send(source, "Notifications disabled.");
            return;
        }
        send(source, "Usage: /f notify <on|off|history>");
    }

    private void handleAdmin(CommandSource source, String[] parts) {
        if (!requireAdmin(source)) {
            return;
        }
        if (parts.length < 2) {
            send(source, "Usage: /f admin <unclaim|setpower|transfer>");
            return;
        }
        String sub = parts[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "unclaim" -> handleAdminUnclaim(source, parts);
            case "setpower" -> handleAdminSetPower(source, parts);
            case "transfer" -> handleAdminTransfer(source, parts);
            default -> send(source, "Unknown admin subcommand.");
        }
    }

    private void handleAdminUnclaim(CommandSource source, String[] parts) {
        ClaimKey claim;
        if (parts.length >= 5) {
            String world = parts[2];
            Integer x = parseInt(parts[3]);
            Integer z = parseInt(parts[4]);
            if (x == null || z == null) {
                send(source, "Usage: /f admin unclaim <world> <x> <z>");
                return;
            }
            claim = new ClaimKey(world, x, z);
        } else {
            UUID playerId = requirePlayer(source);
            if (playerId == null) {
                return;
            }
            Optional<Location> location = server.getPlayerLocation(playerId);
            if (location.isEmpty()) {
                send(source, "Location unavailable.");
                return;
            }
            claim = ClaimKey.fromLocation(location.get(), settings.chunkSize);
        }
        Result<Void> result = service.adminUnclaim(claim);
        send(source, result.message());
    }

    private void handleAdminSetPower(CommandSource source, String[] parts) {
        if (parts.length < 4) {
            send(source, "Usage: /f admin setpower <faction> <value|clear>");
            return;
        }
        String factionName = parts[2];
        Integer value = null;
        if (!parts[3].equalsIgnoreCase("clear")) {
            value = parseInt(parts[3]);
            if (value == null) {
                send(source, "Power must be a number or 'clear'.");
                return;
            }
        }
        Result<Void> result = service.adminSetPower(factionName, value);
        send(source, result.message());
    }

    private void handleAdminTransfer(CommandSource source, String[] parts) {
        if (parts.length < 4) {
            send(source, "Usage: /f admin transfer <faction> <player>");
            return;
        }
        String factionName = parts[2];
        Optional<UUID> targetId = server.resolvePlayerId(parts[3]);
        if (targetId.isEmpty()) {
            send(source, "Player not found.");
            return;
        }
        Result<Void> result = service.adminTransferLeadership(factionName, targetId.get());
        send(source, result.message());
    }

    private void handleReload(CommandSource source) {
        if (!requireAdmin(source)) {
            return;
        }
        if (reloadHandler == null) {
            send(source, "Reload is not available.");
            return;
        }
        Result<Void> result = reloadHandler.get();
        send(source, result.message());
    }

    private void handleDisband(CommandSource source) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        Result<Void> result = service.disband(playerId);
        send(source, result.message());
    }

    private void handleLeave(CommandSource source) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        Result<Void> result = service.leave(playerId);
        send(source, result.message());
    }

    private void handleInvite(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 2) {
            send(source, "Usage: /f invite <player>");
            return;
        }
        Optional<UUID> targetId = server.resolvePlayerId(parts[1]);
        if (targetId.isEmpty()) {
            send(source, "Player not found.");
            return;
        }
        Result<Void> result = service.invite(playerId, targetId.get());
        send(source, result.message());
        if (result.ok()) {
            server.sendMessage(targetId.get(), PREFIX + "You have been invited to a faction. Use /f accept <name>.");
        }
    }

    private void handleAccept(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 2) {
            send(source, "Usage: /f accept <faction>");
            return;
        }
        Result<Void> result = service.acceptInvite(playerId, parts[1]);
        send(source, result.message());
    }

    private void handleDeny(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 2) {
            send(source, "Usage: /f deny <faction>");
            return;
        }
        Result<Void> result = service.denyInvite(playerId, parts[1]);
        send(source, result.message());
    }

    private void handleKick(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 2) {
            send(source, "Usage: /f kick <player>");
            return;
        }
        Optional<UUID> targetId = server.resolvePlayerId(parts[1]);
        if (targetId.isEmpty()) {
            send(source, "Player not found.");
            return;
        }
        Result<Void> result = service.kick(playerId, targetId.get());
        send(source, result.message());
        if (result.ok()) {
            server.sendMessage(targetId.get(), PREFIX + "You were kicked from your faction.");
        }
    }

    private void handlePromote(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 2) {
            send(source, "Usage: /f promote <player>");
            return;
        }
        Optional<UUID> targetId = server.resolvePlayerId(parts[1]);
        if (targetId.isEmpty()) {
            send(source, "Player not found.");
            return;
        }
        Result<Void> result = service.promote(playerId, targetId.get());
        send(source, result.message());
        if (result.ok() && memberRoleChangeHandler != null) {
            Optional<Faction> faction = service.findFactionByMember(targetId.get());
            if (faction.isPresent()) {
                MemberRole role = faction.get().roleOf(targetId.get());
                if (role != null) {
                    memberRoleChangeHandler.handle(playerId, targetId.get(), role, true);
                }
            }
        }
    }

    private void handleDemote(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 2) {
            send(source, "Usage: /f demote <player>");
            return;
        }
        Optional<UUID> targetId = server.resolvePlayerId(parts[1]);
        if (targetId.isEmpty()) {
            send(source, "Player not found.");
            return;
        }
        Result<Void> result = service.demote(playerId, targetId.get());
        send(source, result.message());
        if (result.ok() && memberRoleChangeHandler != null) {
            Optional<Faction> faction = service.findFactionByMember(targetId.get());
            if (faction.isPresent()) {
                MemberRole role = faction.get().roleOf(targetId.get());
                if (role != null) {
                    memberRoleChangeHandler.handle(playerId, targetId.get(), role, false);
                }
            }
        }
    }

    private void handleLeader(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 2) {
            send(source, "Usage: /f leader <player>");
            return;
        }
        Optional<UUID> targetId = server.resolvePlayerId(parts[1]);
        if (targetId.isEmpty()) {
            send(source, "Player not found.");
            return;
        }
        Result<Void> result = service.transferLeadership(playerId, targetId.get());
        send(source, result.message());
    }

    private void handleAlly(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 2) {
            send(source, "Usage: /f ally <faction>");
            return;
        }
        Result<Void> result = service.ally(playerId, parts[1]);
        send(source, result.message());
    }

    private void handleUnally(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 2) {
            send(source, "Usage: /f unally <faction>");
            return;
        }
        Result<Void> result = service.unally(playerId, parts[1]);
        send(source, result.message());
    }

    private void handleEnemy(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 2) {
            send(source, "Usage: /f enemy <faction>");
            return;
        }
        Result<Void> result = service.enemy(playerId, parts[1]);
        send(source, result.message());
    }

    private void handleUnenemy(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 2) {
            send(source, "Usage: /f unenemy <faction>");
            return;
        }
        Result<Void> result = service.unenemy(playerId, parts[1]);
        send(source, result.message());
    }

    private void handleSetHome(CommandSource source) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        Optional<Location> location = server.getPlayerLocation(playerId);
        if (location.isEmpty()) {
            send(source, "Location unavailable.");
            return;
        }
        Result<Void> result = service.setHome(playerId, location.get());
        send(source, result.message());
    }

    private void handleHome(CommandSource source) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        Result<Location> result = service.getHome(playerId);
        if (!result.ok()) {
            send(source, result.message());
            return;
        }
        Location home = result.value();
        send(source, "Teleportation in 5 seconds... Do not move.");

        server.teleportDelayed(playerId, home, 5,
                () -> server.sendMessage(playerId, PREFIX + "Teleported to home."),
                () -> server.sendMessage(playerId, PREFIX + "Teleportation cancelled: you moved."));
    }

    private void handleClaim(CommandSource source) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        Optional<Location> location = server.getPlayerLocation(playerId);
        if (location.isEmpty()) {
            send(source, "Location unavailable.");
            return;
        }
        ClaimKey claim = ClaimKey.fromLocation(location.get(), settings.chunkSize);
        Result<Void> result = service.claim(playerId, claim);
        send(source, result.message());
        if (result.ok() && claimEffectHandler != null) {
            Optional<UUID> ownerId = service.getClaimOwnerId(claim);
            claimEffectHandler.handle(playerId, location.get(), ClaimChangeType.CLAIM, ownerId);
        }
    }

    private void handleUnclaim(CommandSource source) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        Optional<Location> location = server.getPlayerLocation(playerId);
        if (location.isEmpty()) {
            send(source, "Location unavailable.");
            return;
        }
        ClaimKey claim = ClaimKey.fromLocation(location.get(), settings.chunkSize);
        Optional<UUID> ownerBefore = service.getClaimOwnerId(claim);
        Result<Void> result = service.unclaim(playerId, claim);
        send(source, result.message());
        if (result.ok() && claimEffectHandler != null) {
            claimEffectHandler.handle(playerId, location.get(), ClaimChangeType.UNCLAIM, ownerBefore);
        }
    }

    private void handleChatToggle(CommandSource source) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (chatToggleHandler == null) {
            send(source, "Chat toggle not available.");
            return;
        }
        String newMode = chatToggleHandler.apply(playerId);
        send(source, "Chat mode set to: " + newMode);
    }

    private void handleBorders(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (borderToggleHandler == null) {
            send(source, "Borders view not available.");
            return;
        }
        Integer seconds = null;
        if (parts.length >= 2) {
            try {
                seconds = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                send(source, "Usage: /f borders [seconds]");
                return;
            }
        }
        String message = borderToggleHandler.apply(playerId, seconds);
        if (message != null && !message.isBlank()) {
            send(source, message);
        }
    }

    // ==================== COMBAT & WAR COMMANDS ====================

    private void handleStats(CommandSource source, String[] parts) {
        if (combatService == null) {
            send(source, "Combat system not available.");
            return;
        }

        UUID targetId;
        if (parts.length >= 2) {
            Optional<UUID> resolved = server.resolvePlayerId(parts[1]);
            if (resolved.isEmpty()) {
                send(source, "Player not found.");
                return;
            }
            targetId = resolved.get();
        } else {
            UUID playerId = requirePlayer(source);
            if (playerId == null) {
                return;
            }
            targetId = playerId;
        }

        String targetName = resolveName(targetId);
        MemberCombatStats stats = combatService.getMemberStats(targetId);
        Optional<Faction> faction = service.findFactionByMember(targetId);

        send(source, "=== Stats: " + targetName + " ===");
        send(source, "Faction: " + faction.map(Faction::name).orElse("None"));
        send(source, "Kills: " + stats.kills() + " | Deaths: " + stats.deaths() + " | K/D: " + stats.kdr());
        send(source, "Faction Kills: " + stats.factionKills() + " | Enemy Kills: " + stats.enemyKills());
        send(source, "Current Streak: " + stats.currentStreak() + " | Best Streak: " + stats.bestStreak());

        if (faction.isPresent()) {
            FactionCombatStats factionStats = combatService.getFactionStats(faction.get().id());
            send(source, "--- Faction Stats ---");
            send(source, "Total Kills: " + factionStats.totalKills() + " | Deaths: " + factionStats.totalDeaths());
            send(source, "Wars Won: " + factionStats.warsWon() + " | Lost: " + factionStats.warsLost() + " | Draw: "
                    + factionStats.warsDraw());
        }
    }

    private void handleTop(CommandSource source, String[] parts) {
        if (combatService == null) {
            send(source, "Combat system not available.");
            return;
        }

        String category = parts.length >= 2 ? parts[1].toLowerCase(Locale.ROOT) : "kills";
        int limit = 10;

        switch (category) {
            case "kills" -> {
                send(source, "=== Top " + limit + " Killers ===");
                List<MemberCombatStats> top = combatService.getTopKillers(limit);
                int rank = 1;
                for (MemberCombatStats stats : top) {
                    String name = resolveName(stats.playerId());
                    send(source, rank + ". " + name + " - " + stats.kills() + " kills (K/D: " + stats.kdr() + ")");
                    rank++;
                }
            }
            case "kdr" -> {
                send(source, "=== Top " + limit + " by K/D Ratio ===");
                List<MemberCombatStats> top = combatService.getTopByKdr(limit);
                int rank = 1;
                for (MemberCombatStats stats : top) {
                    String name = resolveName(stats.playerId());
                    send(source, rank + ". " + name + " - K/D: " + stats.kdr() + " (" + stats.kills() + "/"
                            + stats.deaths() + ")");
                    rank++;
                }
            }
            case "factions" -> {
                send(source, "=== Top " + limit + " Factions ===");
                List<FactionCombatStats> top = combatService.getTopFactions(limit);
                int rank = 1;
                for (FactionCombatStats stats : top) {
                    String name = service.getFactionById(stats.factionId()).map(Faction::name).orElse("Unknown");
                    send(source, rank + ". " + name + " - " + stats.totalKills() + " kills, " + stats.warsWon()
                            + " wars won");
                    rank++;
                }
            }
            default -> send(source, "Usage: /f top <kills|kdr|factions>");
        }
    }

    private void handleWar(CommandSource source, String[] parts) {
        if (combatService == null) {
            send(source, "Combat system not available.");
            return;
        }

        if (parts.length < 2) {
            handleWarStatus(source);
            return;
        }

        String sub = parts[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "declare" -> handleWarDeclare(source, parts);
            case "surrender" -> handleWarSurrender(source);
            case "status" -> handleWarStatus(source);
            default -> send(source, "Usage: /f war <declare|surrender|status>");
        }
    }

    private void handleWarDeclare(CommandSource source, String[] parts) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        if (parts.length < 3) {
            send(source, "Usage: /f war declare <faction>");
            return;
        }
        Result<War> result = combatService.declareWar(playerId, parts[2]);
        send(source, result.message());

        if (result.ok() && warDeclareHandler != null) {
            warDeclareHandler.handle(playerId, result.value());
        }

        // Notify defenders
        if (result.ok()) {
            War war = result.value();
            Optional<Faction> defenderFaction = service.getFactionById(war.defenderFactionId());
            Optional<Faction> attackerFaction = service.getFactionById(war.attackerFactionId());
            if (defenderFaction.isPresent() && attackerFaction.isPresent()) {
                String attackerName = attackerFaction.get().name();
                for (UUID memberId : defenderFaction.get().members().keySet()) {
                    server.sendMessage(memberId, PREFIX + "GUERRE! " + attackerName + " vous a déclaré la guerre!");
                }
            }
        }
    }

    private void handleWarSurrender(CommandSource source) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }
        Result<Void> result = combatService.surrender(playerId);
        send(source, result.message());
    }

    private void handleWarStatus(CommandSource source) {
        UUID playerId = requirePlayer(source);
        if (playerId == null) {
            return;
        }

        Optional<Faction> factionOpt = service.findFactionByMember(playerId);
        if (factionOpt.isEmpty()) {
            send(source, "You are not in a faction.");
            return;
        }

        Optional<War> warOpt = combatService.getActiveWar(factionOpt.get().id());
        if (warOpt.isEmpty()) {
            send(source, "Your faction is not at war.");
            return;
        }

        War war = warOpt.get();
        String attackerName = service.getFactionById(war.attackerFactionId()).map(Faction::name).orElse("Unknown");
        String defenderName = service.getFactionById(war.defenderFactionId()).map(Faction::name).orElse("Unknown");

        send(source, "=== War Status ===");
        send(source, attackerName + " vs " + defenderName);
        send(source, "State: " + war.state().name());
        send(source, "Points: " + attackerName + " " + war.attackerPoints() + " - " + war.defenderPoints() + " "
                + defenderName);
        send(source, "Kills: " + attackerName + " " + war.attackerKills() + " - " + war.defenderKills() + " "
                + defenderName);

        if (war.state() == War.WarState.PENDING) {
            long remainingMs = war.gracePeriodEnd() - server.nowEpochMs();
            long remainingSeconds = Math.max(0, remainingMs / 1000);
            send(source, "Grace period: " + remainingSeconds + "s remaining");
        }
    }

    private UUID requirePlayer(CommandSource source) {
        Optional<UUID> playerId = source.playerId();
        if (playerId.isEmpty()) {
            send(source, "This command is player-only.");
            return null;
        }
        return playerId.get();
    }

    private void sendHelp(CommandSource source) {
        send(source,
                "Commands: create, disband, list, info, who, map, invites, notify, desc, rename, invite, accept, deny, leave, kick, promote, demote, leader, ally, unally, enemy, unenemy, sethome, home, claim, unclaim, chat, borders, stats, top, war, reload, admin");
    }

    private void send(CommandSource source, String message) {
        source.sendMessage(PREFIX + message);
    }

    private UUID findLeaderId(Faction faction) {
        for (var entry : faction.members().entrySet()) {
            if (entry.getValue() == MemberRole.LEADER) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String resolveName(UUID id) {
        Optional<String> name = server.resolvePlayerName(id);
        return name.filter(n -> !n.isBlank()).orElse(shortId(id));
    }

    private String formatRole(MemberRole role) {
        if (role == null) {
            return "member";
        }
        return switch (role) {
            case LEADER -> "leader";
            case OFFICER -> "officer";
            case MEMBER -> "member";
            case RECRUIT -> "recruit";
        };
    }

    private String shortId(UUID id) {
        String raw = id.toString();
        return raw.substring(0, Math.min(raw.length(), 8));
    }

    private boolean requireAdmin(CommandSource source) {
        if (source.hasPermission("factions.admin")) {
            return true;
        }
        send(source, "You do not have permission for this command.");
        return false;
    }

    private Integer parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatLocation(Location location) {
        return location.world() + " (" + location.x() + ", " + location.y() + ", " + location.z() + ")";
    }
}
