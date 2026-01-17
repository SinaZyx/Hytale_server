package com.kingc.hytale.duels.kit;

import com.kingc.hytale.duels.api.PlayerRef;
import com.kingc.hytale.duels.api.ServerAdapter;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

public final class KitService {
    private final KitRepository repository;
    private final ServerAdapter server;

    public KitService(KitRepository repository, ServerAdapter server) {
        this.repository = repository;
        this.server = server;
    }

    public Optional<KitDefinition> getKit(String id) {
        return repository.get(id);
    }

    public Collection<KitDefinition> getAllKits() {
        return repository.getAll();
    }

    public void createKit(KitDefinition kit) {
        repository.add(kit);
    }

    public boolean deleteKit(String id) {
        return repository.remove(id);
    }

    public boolean kitExists(String id) {
        return repository.exists(id);
    }

    public void applyKit(PlayerRef player, KitDefinition kit) {
        server.clearInventory(player);
        server.clearEffects(player);

        if (kit.helmet() != null || kit.chestplate() != null || kit.leggings() != null || kit.boots() != null) {
            server.setArmor(player, kit.helmet(), kit.chestplate(), kit.leggings(), kit.boots());
        }

        if (kit.items() != null && !kit.items().isEmpty()) {
            server.giveItems(player, kit.items().toArray(new com.kingc.hytale.duels.api.ItemStack[0]));
        }

        if (kit.effects() != null) {
            for (var entry : kit.effects().values()) {
                server.applyEffect(player, entry.effectType(), entry.amplifier(), entry.durationTicks());
            }
        }
    }

    public void save() throws IOException {
        repository.save();
    }
}
