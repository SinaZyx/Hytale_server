package com.kingc.hytale.factions.service;

import com.kingc.hytale.factions.api.Location;
import com.kingc.hytale.factions.model.ClaimKey;
import com.kingc.hytale.factions.model.Faction;
import com.kingc.hytale.factions.model.FactionInvite;
import com.kingc.hytale.factions.model.MemberRole;
import com.kingc.hytale.factions.storage.FactionDataStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class FactionService {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");

    private final FactionDataStore store;
    private final FactionSettings settings;
    private final TimeProvider timeProvider;
    private final ActionLogger actionLogger;
    private final Map<UUID, Long> lastClaimAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastUnclaimAt = new ConcurrentHashMap<>();

    public FactionService(FactionDataStore store, FactionSettings settings, TimeProvider timeProvider, ActionLogger actionLogger) {
        this.store = store;
        this.settings = settings;
        this.timeProvider = timeProvider;
        this.actionLogger = actionLogger;
    }

    public Result<Faction> createFaction(UUID ownerId, String name) {
        String trimmed = normalizeName(name);
        String validationError = validateName(trimmed);
        if (validationError != null) {
            return Result.error(validationError);
        }
        if (findFactionByMember(ownerId).isPresent()) {
            return Result.error("You are already in a faction.");
        }
        if (store.nameIndex().containsKey(FactionDataStore.nameKey(trimmed))) {
            return Result.error("That faction name is already taken.");
        }
        UUID id = UUID.randomUUID();
        Faction faction = new Faction(id, trimmed, timeProvider.nowEpochMs(), Map.of(ownerId, MemberRole.LEADER));
        store.factions().put(id, faction);
        store.nameIndex().put(FactionDataStore.nameKey(trimmed), id);
        logAction(ownerId, "create faction=" + trimmed);
        return Result.ok("Faction created: " + trimmed, faction);
    }

    public Result<Void> disband(UUID actorId) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForDisband())) {
            return Result.error("You do not have permission to disband the faction.");
        }
        UUID factionId = faction.id();
        store.factions().remove(factionId);
        store.nameIndex().remove(FactionDataStore.nameKey(faction.name()));
        store.claims().entrySet().removeIf(entry -> entry.getValue().equals(factionId));
        store.invites().values().forEach(invites -> invites.removeIf(invite -> invite.factionId().equals(factionId)));
        logAction(actorId, "disband faction=" + faction.name());
        return Result.ok("Faction disbanded.", null);
    }

    public Result<Void> rename(UUID actorId, String newName) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForRename())) {
            return Result.error("You do not have permission to rename the faction.");
        }
        String trimmed = normalizeName(newName);
        String validationError = validateName(trimmed);
        if (validationError != null) {
            return Result.error(validationError);
        }
        String newKey = FactionDataStore.nameKey(trimmed);
        if (store.nameIndex().containsKey(newKey)) {
            return Result.error("That faction name is already taken.");
        }
        store.nameIndex().remove(FactionDataStore.nameKey(faction.name()));
        faction.setName(trimmed);
        store.nameIndex().put(newKey, faction.id());
        logAction(actorId, "rename faction=" + trimmed);
        return Result.ok("Faction renamed to " + trimmed + ".", null);
    }

    public Result<Void> invite(UUID actorId, UUID targetId) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForInvite())) {
            return Result.error("You do not have permission to invite.");
        }
        if (faction.isMember(targetId)) {
            return Result.error("That player is already in your faction.");
        }
        if (findFactionByMember(targetId).isPresent()) {
            return Result.error("That player is already in a faction.");
        }
        if (faction.members().size() >= settings.maxMembers) {
            return Result.error("Your faction is full.");
        }
        long expiresAt = timeProvider.nowEpochMs() + settings.inviteExpiryMinutes * 60_000L;
        List<FactionInvite> invites = store.invites().computeIfAbsent(targetId, ignored -> new ArrayList<>());
        invites.removeIf(invite -> invite.factionId().equals(faction.id()));
        invites.add(new FactionInvite(faction.id(), actorId, expiresAt));
        logAction(actorId, "invite target=" + targetId + " faction=" + faction.name());
        return Result.ok("Invite sent to player.", null);
    }

    public Result<Void> acceptInvite(UUID playerId, String factionName) {
        if (findFactionByMember(playerId).isPresent()) {
            return Result.error("You are already in a faction.");
        }
        Optional<Faction> factionOpt = findFactionByName(factionName);
        if (factionOpt.isEmpty()) {
            return Result.error("Faction not found.");
        }
        Faction faction = factionOpt.get();
        if (faction.members().size() >= settings.maxMembers) {
            return Result.error("That faction is full.");
        }
        List<FactionInvite> invites = store.invites().get(playerId);
        if (invites == null || invites.isEmpty()) {
            return Result.error("You do not have any invites.");
        }
        long now = timeProvider.nowEpochMs();
        invites.removeIf(invite -> invite.isExpired(now));
        Optional<FactionInvite> inviteOpt = invites.stream()
                .filter(invite -> invite.factionId().equals(faction.id()))
                .findFirst();
        if (inviteOpt.isEmpty()) {
            return Result.error("No invite found for that faction.");
        }
        faction.setMemberRole(playerId, MemberRole.RECRUIT);
        invites.remove(inviteOpt.get());
        logAction(playerId, "accept faction=" + faction.name());
        return Result.ok("You joined " + faction.name() + ".", null);
    }

    public Result<Void> denyInvite(UUID playerId, String factionName) {
        List<FactionInvite> invites = store.invites().get(playerId);
        if (invites == null || invites.isEmpty()) {
            return Result.error("You do not have any invites.");
        }
        Optional<Faction> factionOpt = findFactionByName(factionName);
        if (factionOpt.isEmpty()) {
            return Result.error("Faction not found.");
        }
        UUID factionId = factionOpt.get().id();
        boolean removed = invites.removeIf(invite -> invite.factionId().equals(factionId));
        if (!removed) {
            return Result.error("No invite found for that faction.");
        }
        logAction(playerId, "deny faction=" + factionOpt.get().name());
        return Result.ok("Invite removed.", null);
    }

    public Result<Void> leave(UUID playerId) {
        Optional<Faction> factionOpt = findFactionByMember(playerId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Faction faction = factionOpt.get();
        MemberRole role = faction.roleOf(playerId);
        if (role == MemberRole.LEADER && faction.members().size() > 1) {
            return Result.error("Transfer leadership or disband before leaving.");
        }
        if (role == MemberRole.LEADER) {
            return disband(playerId);
        }
        faction.removeMember(playerId);
        logAction(playerId, "leave faction=" + faction.name());
        return Result.ok("You left the faction.", null);
    }

    public Result<Void> kick(UUID actorId, UUID targetId) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForKick())) {
            return Result.error("You do not have permission to kick.");
        }
        MemberRole targetRole = faction.roleOf(targetId);
        if (targetRole == null) {
            return Result.error("That player is not in your faction.");
        }
        MemberRole actorRole = faction.roleOf(actorId);
        if (actorRole.rank() <= targetRole.rank()) {
            return Result.error("You cannot kick a member with equal or higher rank.");
        }
        faction.removeMember(targetId);
        logAction(actorId, "kick target=" + targetId + " faction=" + faction.name());
        return Result.ok("Member kicked.", null);
    }

    public Result<Void> promote(UUID actorId, UUID targetId) {
        return changeRank(actorId, targetId, true);
    }

    public Result<Void> demote(UUID actorId, UUID targetId) {
        return changeRank(actorId, targetId, false);
    }

    public Result<Void> transferLeadership(UUID actorId, UUID targetId) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForLeader())) {
            return Result.error("You do not have permission to transfer leadership.");
        }
        if (faction.roleOf(actorId) != MemberRole.LEADER) {
            return Result.error("Only the leader can transfer leadership.");
        }
        if (!faction.isMember(targetId)) {
            return Result.error("That player is not in your faction.");
        }
        faction.setMemberRole(actorId, MemberRole.OFFICER);
        faction.setMemberRole(targetId, MemberRole.LEADER);
        logAction(actorId, "transfer leader=" + targetId + " faction=" + faction.name());
        return Result.ok("Leadership transferred.", null);
    }

    public Result<Void> setHome(UUID actorId, Location location) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForSetHome())) {
            return Result.error("You do not have permission to set home.");
        }
        faction.setHome(location);
        logAction(actorId, "sethome faction=" + faction.name());
        return Result.ok("Faction home set.", null);
    }

    public Result<Void> setDescription(UUID actorId, String description) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForDescription())) {
            return Result.error("You do not have permission to edit the description.");
        }
        String normalized = normalizeDescription(description);
        if (normalized.isEmpty()) {
            faction.setDescription(null);
            logAction(actorId, "desc clear faction=" + faction.name());
            return Result.ok("Faction description cleared.", null);
        }
        if (normalized.length() > settings.maxDescriptionLength) {
            return Result.error("Description too long (max " + settings.maxDescriptionLength + " characters).");
        }
        faction.setDescription(normalized);
        logAction(actorId, "desc set faction=" + faction.name());
        return Result.ok("Faction description updated.", null);
    }

    public Result<Location> getHome(UUID playerId) {
        Optional<Faction> factionOpt = findFactionByMember(playerId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Location home = factionOpt.get().home();
        if (home == null) {
            return Result.error("Your faction does not have a home yet.");
        }
        return Result.ok("Home ready.", home);
    }

    public Result<Void> claim(UUID actorId, ClaimKey claim) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForClaim())) {
            return Result.error("You do not have permission to claim.");
        }
        if (!isClaimWorldAllowed(claim.world())) {
            return Result.error("Claims are disabled in this world.");
        }
        String cooldownError = checkCooldown(lastClaimAt, settings.claimCooldownSeconds, faction.id(), "claim");
        if (cooldownError != null) {
            return Result.error(cooldownError);
        }
        if (store.claims().containsKey(claim)) {
            return Result.error("That chunk is already claimed.");
        }
        long claimCount = store.claims().values().stream().filter(id -> id.equals(faction.id())).count();
        int claimLimit = getClaimLimit(faction.id());
        if (claimCount >= claimLimit) {
            return Result.error("Your faction has reached the claim limit (" + claimCount + "/" + claimLimit + ").");
        }
        store.claims().put(claim, faction.id());
        lastClaimAt.put(faction.id(), timeProvider.nowEpochMs());
        logAction(actorId, "claim " + claim.toKey() + " faction=" + faction.name());
        return Result.ok("Chunk claimed.", null);
    }

    public Result<Void> unclaim(UUID actorId, ClaimKey claim) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForUnclaim())) {
            return Result.error("You do not have permission to unclaim.");
        }
        String cooldownError = checkCooldown(lastUnclaimAt, settings.unclaimCooldownSeconds, faction.id(), "unclaim");
        if (cooldownError != null) {
            return Result.error(cooldownError);
        }
        UUID ownerId = store.claims().get(claim);
        if (ownerId == null || !ownerId.equals(faction.id())) {
            return Result.error("Your faction does not own this claim.");
        }
        store.claims().remove(claim);
        lastUnclaimAt.put(faction.id(), timeProvider.nowEpochMs());
        logAction(actorId, "unclaim " + claim.toKey() + " faction=" + faction.name());
        return Result.ok("Chunk unclaimed.", null);
    }

    public Result<Void> ally(UUID actorId, String targetFactionName) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForAlly())) {
            return Result.error("You do not have permission to manage allies.");
        }
        Optional<Faction> targetOpt = findFactionByName(targetFactionName);
        if (targetOpt.isEmpty()) {
            return Result.error("Faction not found.");
        }
        Faction target = targetOpt.get();
        if (target.id().equals(faction.id())) {
            return Result.error("You cannot ally with yourself.");
        }
        faction.addAlly(target.id());
        target.addAlly(faction.id());
        logAction(actorId, "ally with=" + target.name() + " faction=" + faction.name());
        return Result.ok("Alliance formed with " + target.name() + ".", null);
    }

    public Result<Void> unally(UUID actorId, String targetFactionName) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForAlly())) {
            return Result.error("You do not have permission to manage allies.");
        }
        Optional<Faction> targetOpt = findFactionByName(targetFactionName);
        if (targetOpt.isEmpty()) {
            return Result.error("Faction not found.");
        }
        Faction target = targetOpt.get();
        faction.removeAlly(target.id());
        target.removeAlly(faction.id());
        logAction(actorId, "unally with=" + target.name() + " faction=" + faction.name());
        return Result.ok("Alliance removed.", null);
    }

    public Result<Void> enemy(UUID actorId, String targetFactionName) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForAlly())) {
            return Result.error("You do not have permission to manage enemies.");
        }
        Optional<Faction> targetOpt = findFactionByName(targetFactionName);
        if (targetOpt.isEmpty()) {
            return Result.error("Faction not found.");
        }
        Faction target = targetOpt.get();
        if (target.id().equals(faction.id())) {
            return Result.error("You cannot declare yourself as an enemy.");
        }
        // Remove from allies if present
        faction.removeAlly(target.id());
        target.removeAlly(faction.id());
        // Add as enemies
        faction.addEnemy(target.id());
        target.addEnemy(faction.id());
        logAction(actorId, "enemy with=" + target.name() + " faction=" + faction.name());
        return Result.ok("You are now enemies with " + target.name() + ".", null);
    }

    public Result<Void> unenemy(UUID actorId, String targetFactionName) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForAlly())) {
            return Result.error("You do not have permission to manage enemies.");
        }
        Optional<Faction> targetOpt = findFactionByName(targetFactionName);
        if (targetOpt.isEmpty()) {
            return Result.error("Faction not found.");
        }
        Faction target = targetOpt.get();
        faction.removeEnemy(target.id());
        target.removeEnemy(faction.id());
        logAction(actorId, "unenemy with=" + target.name() + " faction=" + faction.name());
        return Result.ok("You are no longer enemies with " + target.name() + ".", null);
    }

    public boolean canBuild(UUID actorId, Location location) {
        ClaimKey claim = ClaimKey.fromLocation(location, settings.chunkSize);
        UUID ownerId = store.claims().get(claim);
        if (ownerId == null) {
            return true;
        }
        Optional<Faction> actorFaction = findFactionByMember(actorId);
        if (actorFaction.isEmpty()) {
            return false;
        }
        if (ownerId.equals(actorFaction.get().id())) {
            return true;
        }
        if (settings.allowAllyBuild) {
            return actorFaction.get().allies().contains(ownerId);
        }
        return false;
    }

    public boolean canDamage(UUID attackerId, UUID targetId) {
        if (settings.allowFriendlyFire) {
            return true;
        }
        Optional<Faction> attackerFaction = findFactionByMember(attackerId);
        Optional<Faction> targetFaction = findFactionByMember(targetId);
        if (attackerFaction.isEmpty() || targetFaction.isEmpty()) {
            return true;
        }
        if (attackerFaction.get().id().equals(targetFaction.get().id())) {
            return false;
        }
        return !attackerFaction.get().allies().contains(targetFaction.get().id());
    }

    public Optional<UUID> getClaimOwnerId(Location location) {
        if (location == null) {
            return Optional.empty();
        }
        ClaimKey claim = ClaimKey.fromLocation(location, settings.chunkSize);
        return Optional.ofNullable(store.claims().get(claim));
    }

    public Optional<UUID> getClaimOwnerId(ClaimKey claim) {
        if (claim == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.claims().get(claim));
    }

    public Optional<Faction> getFactionById(UUID factionId) {
        if (factionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.factions().get(factionId));
    }

    public List<Faction> getAllFactions() {
        return store.factions().values().stream()
                .sorted(Comparator.comparing(Faction::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public int getClaimCount(UUID factionId) {
        if (factionId == null) {
            return 0;
        }
        int count = 0;
        for (UUID ownerId : store.claims().values()) {
            if (factionId.equals(ownerId)) {
                count++;
            }
        }
        return count;
    }

    public int getPower(UUID factionId) {
        if (factionId == null) {
            return 0;
        }
        Faction faction = store.factions().get(factionId);
        if (faction == null) {
            return 0;
        }
        Integer override = faction.powerOverride();
        int power;
        if (override != null) {
            power = override;
        } else {
            int members = faction.members().size();
            power = settings.basePower + (settings.powerPerMember * members);
        }
        if (settings.maxPower > 0) {
            power = Math.min(power, settings.maxPower);
        }
        return Math.max(power, 0);
    }

    public int getClaimLimit(UUID factionId) {
        if (factionId == null) {
            return 0;
        }
        Faction faction = store.factions().get(factionId);
        if (faction == null) {
            return 0;
        }
        int members = faction.members().size();
        int limit = settings.baseClaimLimit + (settings.claimLimitPerMember * members);
        if (settings.maxClaims > 0) {
            limit = Math.min(limit, settings.maxClaims);
        }
        return Math.max(limit, 0);
    }

    public ClaimMap buildClaimMap(Location location, UUID viewerId, int radius) {
        if (location == null) {
            return new ClaimMap("", 0, 0, List.of(), "");
        }
        int clampedRadius = Math.min(Math.max(radius, 1), 10);
        ClaimKey center = ClaimKey.fromLocation(location, settings.chunkSize);
        Optional<Faction> viewerFaction = viewerId == null ? Optional.empty() : findFactionByMember(viewerId);
        UUID viewerFactionId = viewerFaction.map(Faction::id).orElse(null);
        Set<UUID> allies = viewerFaction.map(Faction::allies).orElse(Set.of());

        List<String> lines = new ArrayList<>();
        String border = "+" + "-".repeat(clampedRadius * 2 + 1) + "+";
        lines.add(border);
        for (int dz = clampedRadius; dz >= -clampedRadius; dz--) {
            StringBuilder row = new StringBuilder();
            row.append('|');
            for (int dx = -clampedRadius; dx <= clampedRadius; dx++) {
                ClaimKey key = new ClaimKey(center.world(), center.x() + dx, center.z() + dz);
                Optional<UUID> ownerId = getClaimOwnerId(key);
                char symbol = '.';
                if (ownerId.isPresent()) {
                    UUID owner = ownerId.get();
                    if (viewerFactionId != null && owner.equals(viewerFactionId)) {
                        symbol = 'F';
                    } else if (allies.contains(owner)) {
                        symbol = 'A';
                    } else {
                        symbol = 'E';
                    }
                }
                if (dx == 0 && dz == 0) {
                    symbol = 'P';
                }
                row.append(symbol);
            }
            row.append('|');
            lines.add(row.toString());
        }
        lines.add(border);
        String legend = "Legend: P=you, F=your claims, A=allies, E=enemy, .=wilderness";
        return new ClaimMap(center.world(), center.x(), center.z(), lines, legend);
    }

    public List<FactionInvite> getInvites(UUID playerId) {
        if (playerId == null) {
            return List.of();
        }
        List<FactionInvite> invites = store.invites().get(playerId);
        if (invites == null || invites.isEmpty()) {
            return List.of();
        }
        long now = timeProvider.nowEpochMs();
        invites.removeIf(invite -> invite.isExpired(now));
        return List.copyOf(invites);
    }

    public boolean isNotificationsEnabled(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        boolean defaultEnabled = Boolean.TRUE.equals(settings.notifyEnabledByDefault);
        Boolean optOut = store.notifyOptOut().get(playerId);
        if (optOut == null) {
            return defaultEnabled;
        }
        return !optOut;
    }

    public void setNotificationsEnabled(UUID playerId, boolean enabled) {
        if (playerId == null) {
            return;
        }
        boolean defaultEnabled = Boolean.TRUE.equals(settings.notifyEnabledByDefault);
        if (enabled == defaultEnabled) {
            store.notifyOptOut().remove(playerId);
        } else {
            store.notifyOptOut().put(playerId, !enabled);
        }
    }

    public Result<Void> adminUnclaim(ClaimKey claim) {
        if (claim == null) {
            return Result.error("Claim not found.");
        }
        UUID owner = store.claims().remove(claim);
        if (owner == null) {
            return Result.error("Claim not found.");
        }
        logAction(null, "admin unclaim " + claim.toKey() + " owner=" + owner);
        return Result.ok("Claim removed.", null);
    }

    public Result<Void> adminSetPower(String factionName, Integer power) {
        Optional<Faction> factionOpt = findFactionByName(factionName);
        if (factionOpt.isEmpty()) {
            return Result.error("Faction not found.");
        }
        Faction faction = factionOpt.get();
        faction.setPowerOverride(power);
        if (power == null) {
            logAction(null, "admin power clear faction=" + faction.name());
            return Result.ok("Faction power override cleared.", null);
        }
        logAction(null, "admin power set faction=" + faction.name() + " power=" + power);
        return Result.ok("Faction power override set to " + power + ".", null);
    }

    public Result<Void> adminTransferLeadership(String factionName, UUID newLeaderId) {
        Optional<Faction> factionOpt = findFactionByName(factionName);
        if (factionOpt.isEmpty()) {
            return Result.error("Faction not found.");
        }
        if (newLeaderId == null) {
            return Result.error("Player not found.");
        }
        Faction faction = factionOpt.get();
        UUID currentLeader = null;
        for (Map.Entry<UUID, MemberRole> entry : faction.members().entrySet()) {
            if (entry.getValue() == MemberRole.LEADER) {
                currentLeader = entry.getKey();
                break;
            }
        }
        if (!faction.isMember(newLeaderId)) {
            faction.setMemberRole(newLeaderId, MemberRole.OFFICER);
        }
        if (currentLeader != null && !currentLeader.equals(newLeaderId)) {
            faction.setMemberRole(currentLeader, MemberRole.OFFICER);
        }
        faction.setMemberRole(newLeaderId, MemberRole.LEADER);
        logAction(null, "admin transfer faction=" + faction.name() + " leader=" + newLeaderId);
        return Result.ok("Leadership updated.", null);
    }

    public Optional<Faction> findFactionByMember(UUID playerId) {
        for (Faction faction : store.factions().values()) {
            if (faction.isMember(playerId)) {
                return Optional.of(faction);
            }
        }
        return Optional.empty();
    }

    public Optional<Faction> findFactionByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        UUID id = store.nameIndex().get(FactionDataStore.nameKey(normalizeName(name)));
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.factions().get(id));
    }

    private Result<Void> changeRank(UUID actorId, UUID targetId, boolean promote) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("You are not in a faction.");
        }
        Faction faction = factionOpt.get();
        MemberRole requiredRole = promote ? settings.roleForPromote() : settings.roleForDemote();
        if (!hasAtLeastRole(faction, actorId, requiredRole)) {
            return Result.error("You do not have permission to change ranks.");
        }
        MemberRole targetRole = faction.roleOf(targetId);
        if (targetRole == null) {
            return Result.error("That player is not in your faction.");
        }
        if (targetRole == MemberRole.LEADER) {
            return Result.error("You cannot change the leader rank.");
        }
        MemberRole actorRole = faction.roleOf(actorId);
        if (actorRole == null || actorRole.rank() <= targetRole.rank()) {
            return Result.error("You cannot change a member with equal or higher rank.");
        }
        MemberRole newRole = promote ? targetRole.promote() : targetRole.demote();
        if (newRole == targetRole) {
            return Result.error("No rank change available.");
        }
        faction.setMemberRole(targetId, newRole);
        String action = promote ? "promote" : "demote";
        logAction(actorId, action + " target=" + targetId + " role=" + newRole.name());
        return Result.ok("Member rank updated.", null);
    }

    private boolean hasAtLeastRole(Faction faction, UUID playerId, MemberRole role) {
        MemberRole current = faction.roleOf(playerId);
        return current != null && current.atLeast(role);
    }

    private boolean isClaimWorldAllowed(String world) {
        if (world == null) {
            return false;
        }
        if (!settings.claimWorldAllowList.isEmpty()) {
            boolean allowed = settings.claimWorldAllowList.stream()
                    .anyMatch(name -> name.equalsIgnoreCase(world));
            if (!allowed) {
                return false;
            }
        }
        return settings.claimWorldDenyList.stream()
                .noneMatch(name -> name.equalsIgnoreCase(world));
    }

    private String checkCooldown(Map<UUID, Long> cooldowns, int cooldownSeconds, UUID factionId, String action) {
        if (cooldownSeconds <= 0) {
            return null;
        }
        long now = timeProvider.nowEpochMs();
        Long last = cooldowns.get(factionId);
        if (last == null) {
            return null;
        }
        long remainingMs = (cooldownSeconds * 1000L) - (now - last);
        if (remainingMs <= 0) {
            return null;
        }
        long remainingSeconds = (remainingMs + 999) / 1000;
        return "You must wait " + remainingSeconds + "s before using " + action + " again.";
    }

    private void logAction(UUID actorId, String action) {
        if (!Boolean.TRUE.equals(settings.actionLogEnabled)) {
            return;
        }
        if (actionLogger == null || action == null || action.isBlank()) {
            return;
        }
        String actor = actorId == null ? "system" : actorId.toString();
        actionLogger.log("actor=" + actor + " " + action);
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            return "Faction name is required.";
        }
        if (name.length() < settings.minNameLength || name.length() > settings.maxNameLength) {
            return "Faction name must be between " + settings.minNameLength + " and " + settings.maxNameLength + " characters.";
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            return "Faction name must be alphanumeric or underscore only.";
        }
        return null;
    }

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return "";
        }
        String trimmed = description.trim();
        if (trimmed.equalsIgnoreCase("clear") || trimmed.equals("-")) {
            return "";
        }
        return trimmed;
    }

    public static final class ClaimMap {
        private final String world;
        private final int centerX;
        private final int centerZ;
        private final List<String> lines;
        private final String legend;

        public ClaimMap(String world, int centerX, int centerZ, List<String> lines, String legend) {
            this.world = world;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.lines = lines;
            this.legend = legend;
        }

        public String world() {
            return world;
        }

        public int centerX() {
            return centerX;
        }

        public int centerZ() {
            return centerZ;
        }

        public List<String> lines() {
            return lines;
        }

        public String legend() {
            return legend;
        }
    }
}
