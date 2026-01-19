package com.kingc.hytale.duels.kit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class KitRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path filePath;
    private final Map<String, KitDefinition> kits = new LinkedHashMap<>();
    private boolean dirty;

    public KitRepository(Path filePath) throws IOException {
        this.filePath = filePath;
        load();
    }

    private void load() throws IOException {
        if (!Files.exists(filePath)) {
            createDefaults();
            save();
            return;
        }
        String json = Files.readString(filePath);
        Map<String, KitDefinition> loaded = GSON.fromJson(json, new TypeToken<Map<String, KitDefinition>>() {}.getType());
        if (loaded != null) {
            kits.putAll(loaded);
        }
    }

    private void createDefaults() {
        // Kit Archer par defaut
        add(KitDefinition.builder("archer")
            .displayName("Archer")
            .iconItem("hytale:bow")
            .armor(
                new com.kingc.hytale.duels.api.ItemStack("hytale:leather_helmet"),
                new com.kingc.hytale.duels.api.ItemStack("hytale:leather_chestplate"),
                new com.kingc.hytale.duels.api.ItemStack("hytale:leather_leggings"),
                new com.kingc.hytale.duels.api.ItemStack("hytale:leather_boots")
            )
            .items(java.util.List.of(
                new com.kingc.hytale.duels.api.ItemStack("hytale:bow"),
                new com.kingc.hytale.duels.api.ItemStack("hytale:arrow", 64),
                new com.kingc.hytale.duels.api.ItemStack("hytale:stone_sword")
            ))
            .build());

        // Kit Tank par defaut
        add(KitDefinition.builder("tank")
            .displayName("Tank")
            .iconItem("hytale:iron_chestplate")
            .armor(
                new com.kingc.hytale.duels.api.ItemStack("hytale:iron_helmet"),
                new com.kingc.hytale.duels.api.ItemStack("hytale:iron_chestplate"),
                new com.kingc.hytale.duels.api.ItemStack("hytale:iron_leggings"),
                new com.kingc.hytale.duels.api.ItemStack("hytale:iron_boots")
            )
            .items(java.util.List.of(
                new com.kingc.hytale.duels.api.ItemStack("hytale:iron_sword"),
                new com.kingc.hytale.duels.api.ItemStack("hytale:shield")
            ))
            .build());

        // Kit Berserker par defaut
        add(KitDefinition.builder("berserker")
            .displayName("Berserker")
            .iconItem("hytale:diamond_axe")
            .armor(
                new com.kingc.hytale.duels.api.ItemStack("hytale:chainmail_helmet"),
                new com.kingc.hytale.duels.api.ItemStack("hytale:chainmail_chestplate"),
                new com.kingc.hytale.duels.api.ItemStack("hytale:chainmail_leggings"),
                new com.kingc.hytale.duels.api.ItemStack("hytale:chainmail_boots")
            )
            .items(java.util.List.of(
                new com.kingc.hytale.duels.api.ItemStack("hytale:diamond_axe")
            ))
            .effects(Map.of("strength", new KitDefinition.EffectEntry("strength", 0, -1)))
            .build());
    }

    public void save() throws IOException {
        if (!dirty && Files.exists(filePath)) {
            return;
        }
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, GSON.toJson(kits));
        dirty = false;
    }

    public Optional<KitDefinition> get(String id) {
        return Optional.ofNullable(kits.get(id.toLowerCase()));
    }

    public Collection<KitDefinition> getAll() {
        return kits.values();
    }

    public void add(KitDefinition kit) {
        kits.put(kit.id().toLowerCase(), kit);
        dirty = true;
    }

    public boolean remove(String id) {
        boolean removed = kits.remove(id.toLowerCase()) != null;
        if (removed) {
            dirty = true;
        }
        return removed;
    }

    public boolean exists(String id) {
        return kits.containsKey(id.toLowerCase());
    }
}
