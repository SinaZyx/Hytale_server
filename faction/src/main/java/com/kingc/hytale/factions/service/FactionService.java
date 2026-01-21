package com.kingc.hytale.factions.service;

import com.kingc.hytale.factions.api.ClaimChangeType;
import com.kingc.hytale.factions.api.Location;
import com.kingc.hytale.factions.api.event.FactionClaimChangedEvent;
import com.kingc.hytale.factions.api.event.FactionCreatedEvent;
import com.kingc.hytale.factions.api.event.FactionDisbandedEvent;
import com.kingc.hytale.factions.api.event.FactionEvent;
import com.kingc.hytale.factions.api.event.FactionEventBus;
import com.kingc.hytale.factions.api.event.FactionHomeSetEvent;
import com.kingc.hytale.factions.api.event.FactionRenamedEvent;
import com.kingc.hytale.factions.api.event.FactionTreasuryChangedEvent;
import com.kingc.hytale.factions.api.event.MemberRoleChangeType;
import com.kingc.hytale.factions.api.event.MemberRoleChangedEvent;
import com.kingc.hytale.factions.model.ClaimKey;
import com.kingc.hytale.factions.model.Faction;
import com.kingc.hytale.factions.model.FactionInvite;
import com.kingc.hytale.factions.model.MemberRole;
import com.kingc.hytale.factions.model.NotificationEntry;
import com.kingc.hytale.factions.model.NotificationType;
import com.kingc.hytale.factions.storage.FactionDataStore;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
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
    private final FactionEventBus eventBus;
    private final Map<UUID, Long> lastClaimAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastUnclaimAt = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<NotificationEntry>> notificationHistory = new ConcurrentHashMap<>();

    public FactionService(FactionDataStore store, FactionSettings settings, TimeProvider timeProvider,
            ActionLogger actionLogger,
            FactionEventBus eventBus) {
        this.store = store;
        this.settings = settings;
        this.timeProvider = timeProvider;
        this.actionLogger = actionLogger;
        this.eventBus = eventBus;
    }

    public Result<Faction> createFaction(UUID ownerId, String name) {
        String trimmed = normalizeName(name);
        Result<Void> validation = validateName(trimmed);
        if (!validation.ok()) {
            return Result.error(validation.message(), validation.args());
        }
        if (findFactionByMember(ownerId).isPresent()) {
            return Result.error("error.already_in_faction");
        }
        if (store.nameIndex().containsKey(FactionDataStore.nameKey(trimmed))) {
            return Result.error("faction.create.name_taken");
        }
        UUID id = UUID.randomUUID();
        Faction faction = new Faction(id, trimmed, timeProvider.nowEpochMs(), Map.of(ownerId, MemberRole.LEADER));
        store.factions().put(id, faction);
        store.nameIndex().put(FactionDataStore.nameKey(trimmed), id);
        logAction(ownerId, "create faction=" + trimmed);
        postEvent(new FactionCreatedEvent(faction, ownerId));
        return Result.ok("faction.create.success", faction, Map.of("name", trimmed));
    }

    public Result<Void> disband(UUID actorId) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForDisband())) {
            return Result.error("error.disband.permission");
        }
        UUID factionId = faction.id();
        store.factions().remove(factionId);
        store.nameIndex().remove(FactionDataStore.nameKey(faction.name()));
        store.claims().entrySet().removeIf(entry -> entry.getValue().equals(factionId));
        store.invites().values().forEach(invites -> invites.removeIf(invite -> invite.factionId().equals(factionId)));
        logAction(actorId, "disband faction=" + faction.name());
        postEvent(new FactionDisbandedEvent(faction, actorId));
        return Result.ok("faction.disband.success", null, Map.of("name", faction.name()));
    }

    public Result<Void> rename(UUID actorId, String newName) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForRename())) {
            return Result.error("error.rename.permission");
        }
        String trimmed = normalizeName(newName);
        Result<Void> validation = validateName(trimmed);
        if (!validation.ok()) {
            return validation;
        }
        String newKey = FactionDataStore.nameKey(trimmed);
        if (store.nameIndex().containsKey(newKey)) {
            return Result.error("faction.create.name_taken");
        }
        String oldName = faction.name();
        store.nameIndex().remove(FactionDataStore.nameKey(oldName));
        faction.setName(trimmed);
        store.nameIndex().put(newKey, faction.id());
        logAction(actorId, "rename faction=" + trimmed);
        postEvent(new FactionRenamedEvent(faction, oldName, actorId));
        return Result.ok("faction.rename.success", null, Map.of("name", trimmed));
    }

    public Result<Void> invite(UUID actorId, UUID targetId) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForInvite())) {
            return Result.error("error.invite.permission");
        }
        if (faction.isMember(targetId)) {
            return Result.error("error.invite.already_member");
        }
        if (findFactionByMember(targetId).isPresent()) {
            return Result.error("error.invite.target_already_in_faction");
        }
        if (faction.members().size() >= settings.maxMembers) {
            return Result.error("error.invite.faction_full");
        }
        long expiresAt = timeProvider.nowEpochMs() + settings.inviteExpiryMinutes * 60_000L;
        List<FactionInvite> invites = store.invites().computeIfAbsent(targetId, ignored -> new ArrayList<>());
        invites.removeIf(invite -> invite.factionId().equals(faction.id()));
        invites.add(new FactionInvite(faction.id(), actorId, expiresAt));
        logAction(actorId, "invite target=" + targetId + " faction=" + faction.name());
        return Result.ok("faction.invite.sent_success", null);
    }

    public Result<Void> acceptInvite(UUID playerId, String factionName) {
        if (findFactionByMember(playerId).isPresent()) {
            return Result.error("error.already_in_faction");
        }
        Optional<Faction> factionOpt = findFactionByName(factionName);
        if (factionOpt.isEmpty()) {
            return Result.error("error.faction_not_found");
        }
        Faction faction = factionOpt.get();
        if (faction.members().size() >= settings.maxMembers) {
            return Result.error("error.invite.faction_full");
        }
        List<FactionInvite> invites = store.invites().get(playerId);
        if (invites == null || invites.isEmpty()) {
            return Result.error("error.invite.no_invites");
        }
        long now = timeProvider.nowEpochMs();
        invites.removeIf(invite -> invite.isExpired(now));
        Optional<FactionInvite> inviteOpt = invites.stream()
                .filter(invite -> invite.factionId().equals(faction.id()))
                .findFirst();
        if (inviteOpt.isEmpty()) {
            return Result.error("error.invite.not_found");
        }
        faction.setMemberRole(playerId, MemberRole.RECRUIT);
        invites.remove(inviteOpt.get());
        logAction(playerId, "accept faction=" + faction.name());
        return Result.ok("faction.invite.accept.success", null, Map.of("name", faction.name()));
    }

    public Result<Void> denyInvite(UUID playerId, String factionName) {
        List<FactionInvite> invites = store.invites().get(playerId);
        if (invites == null || invites.isEmpty()) {
            return Result.error("error.invite.no_invites");
        }
        Optional<Faction> factionOpt = findFactionByName(factionName);
        if (factionOpt.isEmpty()) {
            return Result.error("error.faction_not_found");
        }
        UUID factionId = factionOpt.get().id();
        boolean removed = invites.removeIf(invite -> invite.factionId().equals(factionId));
        if (!removed) {
            return Result.error("error.invite.not_found");
        }
        logAction(playerId, "deny faction=" + factionOpt.get().name());
        return Result.ok("faction.invite.deny.success", null);
    }

    public Result<Void> leave(UUID playerId) {
        Optional<Faction> factionOpt = findFactionByMember(playerId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        Faction faction = factionOpt.get();
        MemberRole role = faction.roleOf(playerId);
        if (role == MemberRole.LEADER) {
            return Result.error("error.leave.must_transfer");
        }
        faction.removeMember(playerId);
        logAction(playerId, "leave faction=" + faction.name());
        return Result.ok("faction.leave.success", null);
    }

    public Result<Void> kick(UUID actorId, UUID targetId) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForKick())) {
            return Result.error("error.kick.permission");
        }
        MemberRole targetRole = faction.roleOf(targetId);
        if (targetRole == null) {
            return Result.error("error.kick.not_member");
        }
        MemberRole actorRole = faction.roleOf(actorId);
        if (actorRole == null || actorRole.rank() <= targetRole.rank()) {
            return Result.error("error.kick.cannot_kick_higher_rank");
        }
        faction.removeMember(targetId);
        logAction(actorId, "kick target=" + targetId + " faction=" + faction.name());
        return Result.ok("faction.kick.success", null);
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
            return Result.error("error.not_in_faction");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForLeader())) {
            return Result.error("error.transfer.permission");
        }
        if (faction.roleOf(actorId) != MemberRole.LEADER) {
            return Result.error("error.transfer.only_leader");
        }
        if (!faction.isMember(targetId)) {
            return Result.error("error.transfer.not_member");
        }
        MemberRole oldActorRole = faction.roleOf(actorId);
        MemberRole oldTargetRole = faction.roleOf(targetId);
        faction.setMemberRole(actorId, MemberRole.OFFICER);
        faction.setMemberRole(targetId, MemberRole.LEADER);
        logAction(actorId, "transfer leader=" + targetId + " faction=" + faction.name());
        postEvent(new MemberRoleChangedEvent(faction, actorId, actorId, oldActorRole, MemberRole.OFFICER,
                MemberRoleChangeType.TRANSFER));
        postEvent(new MemberRoleChangedEvent(faction, actorId, targetId, oldTargetRole, MemberRole.LEADER,
                MemberRoleChangeType.TRANSFER));
        return Result.ok("faction.transfer.success", null);
    }

    public Result<Void> setHome(UUID actorId, Location location) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForSetHome())) {
            return Result.error("error.home.permission");
        }
        faction.setHome(location);
        logAction(actorId, "sethome faction=" + faction.name());
        postEvent(new FactionHomeSetEvent(faction, location, actorId));
        return Result.ok("faction.home.set.success", null);
    }

    public Optional<Double> getTreasuryBalance(UUID factionId) {
        if (factionId == null) {
            return Optional.empty();
        }
        Faction faction = store.factions().get(factionId);
        if (faction == null) {
            return Optional.empty();
        }
        return Optional.of(faction.treasuryBalance());
    }

    public Result<Double> depositTreasury(UUID actorId, double amount) {
        if (amount <= 0) {
            return Result.error("error.treasury.invalid_amount");
        }
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        return adjustTreasury(factionOpt.get(), actorId, amount);
    }

    public Result<Double> withdrawTreasury(UUID actorId, double amount) {
        if (amount <= 0) {
            return Result.error("error.treasury.invalid_amount");
        }
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        return adjustTreasury(factionOpt.get(), actorId, -amount);
    }

    public Result<Double> adjustTreasury(UUID factionId, UUID actorId, double amount) {
        if (factionId == null) {
            return Result.error("error.faction_not_found");
        }
        Faction faction = store.factions().get(factionId);
        if (faction == null) {
            return Result.error("error.faction_not_found");
        }
        if (amount == 0) {
            return Result.error("error.treasury.invalid_zero");
        }
        return adjustTreasury(faction, actorId, amount);
    }

    public Result<Void> setDescription(UUID actorId, String description) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForDescription())) {
            return Result.error("error.description.permission");
        }
        String normalized = normalizeDescription(description);
        if (normalized.isEmpty()) {
            faction.setDescription(null);
            logAction(actorId, "desc clear faction=" + faction.name());
            return Result.ok("faction.description.cleared", null);
        }
        if (normalized.length() > settings.maxDescriptionLength) {
            return Result.error("error.description.too_long",
                    Map.of("max", String.valueOf(settings.maxDescriptionLength)));
        }
        faction.setDescription(normalized);
        logAction(actorId, "desc set faction=" + faction.name());
        return Result.ok("faction.description.updated", null);
    }

    public Result<Location> getHome(UUID playerId) {
        Optional<Faction> factionOpt = findFactionByMember(playerId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        Location home = factionOpt.get().home();
        if (home == null) {
            return Result.error("error.home.not_set");
        }
        return Result.ok("faction.home.ready", home);
    }

    public Result<Void> claim(UUID actorId, ClaimKey claim) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForClaim())) {
            return Result.error("error.claim.permission");
        }
        if (!isClaimWorldAllowed(claim.world())) {
            return Result.error("error.claim.disabled_world");
        }
        Result<Void> cooldown = checkCooldown(lastClaimAt, settings.claimCooldownSeconds, faction.id(), "claim");
        if (!cooldown.ok()) {
            return cooldown;
        }
        if (store.claims().containsKey(claim)) {
            return Result.error("error.claim.already_claimed");
        }
        long claimCount = store.claims().values().stream().filter(id -> id.equals(faction.id())).count();
        int claimLimit = getClaimLimit(faction.id());
        if (claimCount >= claimLimit) {
            return Result.error("error.claim.limit_reached", Map.of("limit", String.valueOf(claimLimit)));
        }
        store.claims().put(claim, faction.id());
        lastClaimAt.put(faction.id(), timeProvider.nowEpochMs());
        logAction(actorId, "claim " + claim.toKey() + " faction=" + faction.name());
        postEvent(new FactionClaimChangedEvent(faction, claim, ClaimChangeType.CLAIM, actorId));
        return Result.ok("faction.claim.success", null);
    }

    public Result<Void> unclaim(UUID actorId, ClaimKey claim) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForUnclaim())) {
            return Result.error("error.unclaim.permission");
        }
        Result<Void> cooldown = checkCooldown(lastUnclaimAt, settings.unclaimCooldownSeconds, faction.id(), "unclaim");
        if (!cooldown.ok()) {
            return cooldown;
        }
        UUID ownerId = store.claims().get(claim);
        if (ownerId == null || !ownerId.equals(faction.id())) {
            return Result.error("error.unclaim.not_owner");
        }
        store.claims().remove(claim);
        lastUnclaimAt.put(faction.id(), timeProvider.nowEpochMs());
        logAction(actorId, "unclaim " + claim.toKey() + " faction=" + faction.name());
        postEvent(new FactionClaimChangedEvent(faction, claim, ClaimChangeType.UNCLAIM, actorId));
        return Result.ok("faction.unclaim.success", null);
    }

    public Result<Void> ally(UUID actorId, String targetFactionName) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForAlly())) {
            return Result.error("error.ally.permission");
        }
        Optional<Faction> targetOpt = findFactionByName(targetFactionName);
        if (targetOpt.isEmpty()) {
            return Result.error("error.faction_not_found");
        }
        Faction target = targetOpt.get();
        if (target.id().equals(faction.id())) {
            return Result.error("error.ally.self");
        }
        faction.addAlly(target.id());
        target.addAlly(faction.id());
        logAction(actorId, "ally with=" + target.name() + " faction=" + faction.name());
        return Result.ok("faction.ally.success", null, Map.of("name", target.name()));
    }

    public Result<Void> unally(UUID actorId, String targetFactionName) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForAlly())) {
            return Result.error("error.ally.permission");
        }
        Optional<Faction> targetOpt = findFactionByName(targetFactionName);
        if (targetOpt.isEmpty()) {
            return Result.error("error.faction_not_found");
        }
        Faction target = targetOpt.get();
        faction.removeAlly(target.id());
        target.removeAlly(faction.id());
        logAction(actorId, "unally with=" + target.name() + " faction=" + faction.name());
        return Result.ok("faction.unally.success", null, Map.of("name", target.name()));
    }

    public Result<Void> enemy(UUID actorId, String targetFactionName) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForAlly())) {
            return Result.error("error.enemy.permission");
        }
        Optional<Faction> targetOpt = findFactionByName(targetFactionName);
        if (targetOpt.isEmpty()) {
            return Result.error("error.faction_not_found");
        }
        Faction target = targetOpt.get();
        if (target.id().equals(faction.id())) {
            return Result.error("error.enemy.self");
        }
        // Remove from allies if present
        faction.removeAlly(target.id());
        target.removeAlly(faction.id());
        // Add as enemies
        faction.addEnemy(target.id());
        target.addEnemy(faction.id());
        logAction(actorId, "enemy with=" + target.name() + " faction=" + faction.name());
        return Result.ok("faction.enemy.success", null, Map.of("name", target.name()));
    }

    public Result<Void> unenemy(UUID actorId, String targetFactionName) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        Faction faction = factionOpt.get();
        if (!hasAtLeastRole(faction, actorId, settings.roleForAlly())) {
            return Result.error("error.enemy.permission");
        }
        Optional<Faction> targetOpt = findFactionByName(targetFactionName);
        if (targetOpt.isEmpty()) {
            return Result.error("error.faction_not_found");
        }
        Faction target = targetOpt.get();
        faction.removeEnemy(target.id());
        target.removeEnemy(faction.id());
        logAction(actorId, "unenemy with=" + target.name() + " faction=" + faction.name());
        return Result.ok("faction.unenemy.success", null, Map.of("name", target.name()));
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

    public Map<ClaimKey, UUID> getClaimsInRadius(String world, int centerX, int centerZ, int radius) {
        if (world == null || radius < 0) {
            return Map.of();
        }
        int clampedRadius = Math.max(0, radius);
        Map<ClaimKey, UUID> result = new HashMap<>();
        for (int dx = -clampedRadius; dx <= clampedRadius; dx++) {
            for (int dz = -clampedRadius; dz <= clampedRadius; dz++) {
                ClaimKey key = new ClaimKey(world, centerX + dx, centerZ + dz);
                UUID owner = store.claims().get(key);
                if (owner != null) {
                    result.put(key, owner);
                }
            }
        }
        return result;
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

    public void recordNotification(UUID playerId, NotificationType type, String title, String message) {
        if (playerId == null) {
            return;
        }
        long timestamp = timeProvider.nowEpochMs();
        NotificationEntry entry = new NotificationEntry(type, title, message, timestamp);
        Deque<NotificationEntry> queue = notificationHistory.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        queue.addFirst(entry);
        int limit = settings.notificationHistoryLimit;
        while (queue.size() > limit) {
            queue.removeLast();
        }
    }

    public List<NotificationEntry> getNotificationHistory(UUID playerId, NotificationType type, int limit) {
        if (playerId == null) {
            return List.of();
        }
        Deque<NotificationEntry> queue = notificationHistory.get(playerId);
        if (queue == null || queue.isEmpty()) {
            return List.of();
        }
        int max = limit <= 0 ? settings.notificationHistoryLimit : limit;
        List<NotificationEntry> result = new ArrayList<>(Math.min(max, queue.size()));
        for (NotificationEntry entry : queue) {
            if (type == null || entry.type() == type) {
                result.add(entry);
                if (result.size() >= max) {
                    break;
                }
            }
        }
        return result;
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
        MemberRole oldLeaderRole = currentLeader == null ? null : faction.roleOf(currentLeader);
        MemberRole oldNewLeaderRole = faction.roleOf(newLeaderId);
        if (currentLeader != null && !currentLeader.equals(newLeaderId)) {
            faction.setMemberRole(currentLeader, MemberRole.OFFICER);
        }
        faction.setMemberRole(newLeaderId, MemberRole.LEADER);
        logAction(null, "admin transfer faction=" + faction.name() + " leader=" + newLeaderId);
        if (currentLeader != null && oldLeaderRole != null) {
            postEvent(new MemberRoleChangedEvent(faction, null, currentLeader, oldLeaderRole, MemberRole.OFFICER,
                    MemberRoleChangeType.ADMIN));
        }
        postEvent(new MemberRoleChangedEvent(faction, null, newLeaderId, oldNewLeaderRole, MemberRole.LEADER,
                MemberRoleChangeType.ADMIN));
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

    public Map<UUID, Faction> factions() {
        return Collections.unmodifiableMap(store.factions());
    }

    private Result<Double> adjustTreasury(Faction faction, UUID actorId, double amount) {
        double oldBalance = faction.treasuryBalance();
        double newBalance = oldBalance + amount;
        if (newBalance < 0) {
            return Result.error("Faction treasury does not have enough funds.");
        }
        faction.setTreasuryBalance(newBalance);
        logAction(actorId, "treasury amount=" + amount + " faction=" + faction.name());
        postEvent(new FactionTreasuryChangedEvent(faction, actorId, amount, oldBalance, newBalance));
        return Result.ok("Treasury updated.", newBalance);
    }

    private Result<Void> changeRank(UUID actorId, UUID targetId, boolean promote) {
        Optional<Faction> factionOpt = findFactionByMember(actorId);
        if (factionOpt.isEmpty()) {
            return Result.error("error.not_in_faction");
        }
        Faction faction = factionOpt.get();
        MemberRole requiredRole = promote ? settings.roleForPromote() : settings.roleForDemote();
        if (!hasAtLeastRole(faction, actorId, requiredRole)) {
            return Result.error("error.rank.permission");
        }
        MemberRole targetRole = faction.roleOf(targetId);
        if (targetRole == null) {
            return Result.error("error.rank.not_member");
        }
        if (targetRole == MemberRole.LEADER) {
            return Result.error("error.rank.cannot_change_leader");
        }
        MemberRole actorRole = faction.roleOf(actorId);
        if (actorRole == null || actorRole.rank() <= targetRole.rank()) {
            return Result.error("error.rank.cannot_change_higher");
        }
        MemberRole oldRole = targetRole;
        MemberRole newRole = promote ? targetRole.promote() : targetRole.demote();
        if (newRole == targetRole) {
            return Result.error("error.rank.no_change_available");
        }
        faction.setMemberRole(targetId, newRole);
        String action = promote ? "promote" : "demote";
        logAction(actorId, action + " target=" + targetId + " role=" + newRole.name());
        postEvent(new MemberRoleChangedEvent(faction, actorId, targetId, oldRole, newRole,
                promote ? MemberRoleChangeType.PROMOTE : MemberRoleChangeType.DEMOTE));
        return Result.ok("faction.rank.updated", null);
    }

    private boolean hasAtLeastRole(Faction faction, UUID playerId, MemberRole role) {
        MemberRole current = faction.roleOf(playerId);
        return current != null && current.atLeast(role);
    }

    private void postEvent(FactionEvent event) {
        if (eventBus == null || event == null) {
            return;
        }
        eventBus.post(event);
    }

    private Result<Void> validateName(String name) {
        if (name == null || name.isBlank()) {
            return Result.error("error.name.required");
        }
        if (name.length() < settings.minNameLength) {
            return Result.error("error.name_too_short", Map.of("min", String.valueOf(settings.minNameLength)));
        }
        if (name.length() > settings.maxNameLength) {
            return Result.error("error.name_too_long", Map.of("max", String.valueOf(settings.maxNameLength)));
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            return Result.error("error.invalid_name");
        }
        return Result.ok(null, null);
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

    private Result<Void> checkCooldown(Map<UUID, Long> cooldowns, int cooldownSeconds, UUID factionId, String action) {
        if (cooldownSeconds <= 0) {
            return Result.ok(null, null);
        }
        long now = timeProvider.nowEpochMs();
        Long last = cooldowns.get(factionId);
        if (last == null) {
            return Result.ok(null, null);
        }
        long remainingMs = (cooldownSeconds * 1000L) - (now - last);
        if (remainingMs <= 0) {
            return Result.ok(null, null);
        }
        long remainingSeconds = (remainingMs + 999) / 1000;
        return Result.error("error.cooldown", Map.of("seconds", String.valueOf(remainingSeconds), "action", action));
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
