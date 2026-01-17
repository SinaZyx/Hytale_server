package com.kingc.hytale.factions.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kingc.hytale.factions.api.Location;
import com.kingc.hytale.factions.model.ClaimKey;
import com.kingc.hytale.factions.model.Faction;
import com.kingc.hytale.factions.model.FactionInvite;
import com.kingc.hytale.factions.model.MemberRole;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FactionDataStore {
    private final Path path;
    private final Gson gson;

    private final Map<UUID, Faction> factions = new HashMap<>();
    private final Map<String, UUID> nameIndex = new HashMap<>();
    private final Map<UUID, List<FactionInvite>> invites = new HashMap<>();
    private final Map<ClaimKey, UUID> claims = new HashMap<>();
    private final Map<UUID, Boolean> notifyOptOut = new HashMap<>();

    public FactionDataStore(Path path) {
        this.path = path;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public Map<UUID, Faction> factions() {
        return factions;
    }

    public Map<String, UUID> nameIndex() {
        return nameIndex;
    }

    public Map<UUID, List<FactionInvite>> invites() {
        return invites;
    }

    public Map<ClaimKey, UUID> claims() {
        return claims;
    }

    public Map<UUID, Boolean> notifyOptOut() {
        return notifyOptOut;
    }

    public void save() throws IOException {
        StoredData stored = new StoredData();

        for (Faction faction : factions.values()) {
            FactionRecord record = new FactionRecord();
            record.id = faction.id().toString();
            record.name = faction.name();
            record.description = faction.description();
            record.createdAtEpochMs = faction.createdAtEpochMs();
            record.home = faction.home() == null ? null : faction.home().serialize();
            record.powerOverride = faction.powerOverride();
            record.members = new HashMap<>();
            for (Map.Entry<UUID, MemberRole> entry : faction.members().entrySet()) {
                record.members.put(entry.getKey().toString(), entry.getValue().name());
            }
            record.allies = new HashSet<>();
            for (UUID ally : faction.allies()) {
                record.allies.add(ally.toString());
            }
            stored.factions.put(record.id, record);
        }

        for (Map.Entry<UUID, List<FactionInvite>> entry : invites.entrySet()) {
            List<InviteRecord> records = new ArrayList<>();
            for (FactionInvite invite : entry.getValue()) {
                InviteRecord record = new InviteRecord();
                record.factionId = invite.factionId().toString();
                record.inviterId = invite.inviterId().toString();
                record.expiresAtEpochMs = invite.expiresAtEpochMs();
                records.add(record);
            }
            stored.invites.put(entry.getKey().toString(), records);
        }

        for (Map.Entry<ClaimKey, UUID> entry : claims.entrySet()) {
            stored.claims.put(entry.getKey().toKey(), entry.getValue().toString());
        }

        for (Map.Entry<UUID, Boolean> entry : notifyOptOut.entrySet()) {
            stored.notifyOptOut.put(entry.getKey().toString(), entry.getValue());
        }

        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(stored, writer);
        }
    }

    public static String nameKey(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private void load() {
        if (!Files.exists(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            StoredData stored = gson.fromJson(reader, StoredData.class);
            if (stored == null) {
                return;
            }
            for (FactionRecord record : stored.factions.values()) {
                UUID id = UUID.fromString(record.id);
                Map<UUID, MemberRole> members = new HashMap<>();
                if (record.members != null) {
                    for (Map.Entry<String, String> entry : record.members.entrySet()) {
                        members.put(UUID.fromString(entry.getKey()), MemberRole.fromString(entry.getValue()));
                    }
                }
                Set<UUID> allies = new HashSet<>();
                if (record.allies != null) {
                    for (String ally : record.allies) {
                        allies.add(UUID.fromString(ally));
                    }
                }
                Faction faction = new Faction(id, record.name, record.createdAtEpochMs, members, allies);
                faction.setDescription(record.description);
                faction.setHome(Location.deserialize(record.home));
                faction.setPowerOverride(record.powerOverride);
                factions.put(id, faction);
                nameIndex.put(nameKey(record.name), id);
            }

            if (stored.invites != null) {
                for (Map.Entry<String, List<InviteRecord>> entry : stored.invites.entrySet()) {
                    UUID targetId = UUID.fromString(entry.getKey());
                    List<FactionInvite> list = new ArrayList<>();
                    for (InviteRecord record : entry.getValue()) {
                        list.add(new FactionInvite(
                                UUID.fromString(record.factionId),
                                UUID.fromString(record.inviterId),
                                record.expiresAtEpochMs
                        ));
                    }
                    invites.put(targetId, list);
                }
            }

            if (stored.claims != null) {
                for (Map.Entry<String, String> entry : stored.claims.entrySet()) {
                    claims.put(ClaimKey.fromKey(entry.getKey()), UUID.fromString(entry.getValue()));
                }
            }

            if (stored.notifyOptOut != null) {
                for (Map.Entry<String, Boolean> entry : stored.notifyOptOut.entrySet()) {
                    notifyOptOut.put(UUID.fromString(entry.getKey()), entry.getValue());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load faction data", e);
        }
    }

    public static final class StoredData {
        public Map<String, FactionRecord> factions = new HashMap<>();
        public Map<String, List<InviteRecord>> invites = new HashMap<>();
        public Map<String, String> claims = new HashMap<>();
        public Map<String, Boolean> notifyOptOut = new HashMap<>();
    }

    public static final class FactionRecord {
        public String id;
        public String name;
        public String description;
        public long createdAtEpochMs;
        public Map<String, String> members;
        public String home;
        public Set<String> allies;
        public Integer powerOverride;
    }

    public static final class InviteRecord {
        public String factionId;
        public String inviterId;
        public long expiresAtEpochMs;
    }
}
