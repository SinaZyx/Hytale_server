package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.livingentity.LivingEntityEffectSystem;
import com.kingc.hytale.duels.api.ItemStack;
import com.kingc.hytale.duels.api.Location;
import com.kingc.hytale.duels.api.ServerAdapter;

import java.util.Optional;
import java.util.UUID;

public final class HytaleServerAdapter implements ServerAdapter {
    private final HytaleDuelsPlugin plugin;

    public HytaleServerAdapter(HytaleDuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public long nowEpochMs() {
        return System.currentTimeMillis();
    }

    @Override
    public Optional<com.kingc.hytale.duels.api.PlayerRef> getPlayer(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        PlayerRef ref = Universe.get().getPlayer(id);
        if (ref == null) {
            return Optional.empty();
        }
        return Optional.of(new HytalePlayerRef(ref));
    }

    @Override
    public Optional<com.kingc.hytale.duels.api.PlayerRef> getPlayerByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        PlayerRef ref = Universe.get().getPlayerByUsername(name, NameMatching.EXACT_IGNORE_CASE);
        if (ref == null) {
            return Optional.empty();
        }
        return Optional.of(new HytalePlayerRef(ref));
    }

    @Override
    public void giveItems(com.kingc.hytale.duels.api.PlayerRef player, ItemStack... items) {
        if (!(player instanceof HytalePlayerRef hPlayer)) return;
        World world = Universe.get().getWorld(hPlayer.hytale().getWorldUuid());
        if (world == null) return;

        world.execute(() -> {
            var entityStore = world.getEntityStore();
            if (entityStore == null) return;
            var store = entityStore.getStore();
            var ref = entityStore.getRefFromUUID(hPlayer.hytale().getUuid());

            if (store != null && ref != null) {
                Player playerComp = store.getComponent(ref, Player.getComponentType());
                if (playerComp != null) {
                   var hotbar = playerComp.getInventory().getHotbar();
                   for (ItemStack item : items) {
                       com.hypixel.hytale.server.core.inventory.ItemStack hItem =
                           new com.hypixel.hytale.server.core.inventory.ItemStack(item.itemId(), item.count());
                       hotbar.addItemStack(hItem);
                   }
                }
            }
        });
    }

    @Override
    public void setArmor(com.kingc.hytale.duels.api.PlayerRef player, ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
        if (!(player instanceof HytalePlayerRef hPlayer)) return;
        World world = Universe.get().getWorld(hPlayer.hytale().getWorldUuid());
        if (world == null) return;

        world.execute(() -> {
            var entityStore = world.getEntityStore();
            if (entityStore == null) return;
            var store = entityStore.getStore();
            var ref = entityStore.getRefFromUUID(hPlayer.hytale().getUuid());

            if (store != null && ref != null) {
                Player playerComp = store.getComponent(ref, Player.getComponentType());
                if (playerComp != null) {
                    var equipment = playerComp.getInventory().getEquipment();
                    // Assuming equipment slots: 0=Head, 1=Chest, 2=Legs, 3=Boots (Standard convention)
                    // Need to verify slots, but this is a reasonable default implementation attempt
                    // If method names differ (e.g. setHelmet), adapt.
                    // Based on HytaleServer.jar structure, likely direct container access.
                    // For now, logging capability.
                    // System.out.println("Setting armor for " + player.name());
                }
            }
        });
    }

    @Override
    public void teleportToLobby(com.kingc.hytale.duels.api.PlayerRef player) {
        Location lobby = plugin.core().settings().lobbySpawn();
        if (lobby != null) {
            player.teleport(lobby);
        }
    }

    @Override
    public void clearInventory(com.kingc.hytale.duels.api.PlayerRef player) {
        if (!(player instanceof HytalePlayerRef hPlayer)) return;
        World world = Universe.get().getWorld(hPlayer.hytale().getWorldUuid());
        if (world == null) return;

        world.execute(() -> {
            var entityStore = world.getEntityStore();
            if (entityStore != null) {
                var store = entityStore.getStore();
                var ref = entityStore.getRefFromUUID(hPlayer.hytale().getUuid());
                if (store != null && ref != null) {
                    Player playerComp = store.getComponent(ref, Player.getComponentType());
                    if (playerComp != null) {
                        try {
                            var hotbar = playerComp.getInventory().getHotbar();
                            for (short i = 0; i < hotbar.getCapacity(); i++) {
                                com.hypixel.hytale.server.core.inventory.ItemStack item = hotbar.getItemStack(i);
                                if (item != null) {
                                    hotbar.removeItemStack(item);
                                }
                            }
                        } catch (Exception e) {
                            // Log
                        }
                    }
                }
            }
        });
    }

    @Override
    public void applyEffect(com.kingc.hytale.duels.api.PlayerRef player, String effectType, int amplifier, int durationTicks) {
        if (!(player instanceof HytalePlayerRef hPlayer)) return;
        // Placeholder implementation for effects
        // Would typically involve LivingEntityEffectSystem or similar
    }

    @Override
    public void clearEffects(com.kingc.hytale.duels.api.PlayerRef player) {
         // Placeholder
    }

    @Override
    public void showTitle(com.kingc.hytale.duels.api.PlayerRef player, String title, String subtitle, String color, float fadeIn, float stay, float fadeOut) {
        if (!(player instanceof HytalePlayerRef hPlayer)) return;
        PlayerRef ref = hPlayer.hytale();
        if (ref == null) return;

        Message titleMsg = Message.raw(title != null ? title : "");
        Message subtitleMsg = Message.raw(subtitle != null ? subtitle : "");

        if (color != null && !color.isBlank()) {
            titleMsg = titleMsg.color(color);
            subtitleMsg = subtitleMsg.color(color);
        }

        EventTitleUtil.showEventTitleToPlayer(
            ref,
            titleMsg,
            subtitleMsg,
            true,
            EventTitleUtil.DEFAULT_ZONE,
            fadeIn,
            stay,
            fadeOut
        );
    }

    @Override
    public java.util.List<ItemStack> getInventory(com.kingc.hytale.duels.api.PlayerRef player) {
        if (!(player instanceof HytalePlayerRef hPlayer)) return java.util.List.of();

        World world = Universe.get().getWorld(hPlayer.hytale().getWorldUuid());
        if (world == null) return java.util.List.of();

        var entityStore = world.getEntityStore();
        if (entityStore == null || entityStore.getStore() == null) return java.util.List.of();

        var ref = entityStore.getRefFromUUID(hPlayer.hytale().getUuid());
        var store = entityStore.getStore();

        Player playerComp = store.getComponent(ref, Player.getComponentType());
        if (playerComp == null) return java.util.List.of();

        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        var hotbar = playerComp.getInventory().getHotbar();
        for (short i = 0; i < hotbar.getCapacity(); i++) {
            var hItem = hotbar.getItemStack(i);
            if (hItem != null) {
                try {
                    org.bson.BsonDocument doc = com.hypixel.hytale.server.core.inventory.ItemStack.CODEC.encode(hItem, com.hypixel.hytale.codec.EmptyExtraInfo.EMPTY);
                    String id = doc.getString("id").getValue();
                    int count = doc.containsKey("count") ? doc.getInt32("count").getValue() : 1;
                    items.add(new ItemStack(id, count));
                } catch (Exception e) {
                    System.err.println("Failed to parse item: " + e.getMessage());
                }
            }
        }
        return items;
    }

    @Override
    public ItemStack[] getArmor(com.kingc.hytale.duels.api.PlayerRef player) {
        return new ItemStack[4];
    }

    // === Methodes utilitaires ===

    public Optional<Location> getPlayerLocation(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        PlayerRef ref = Universe.get().getPlayer(id);
        if (ref == null) {
            return Optional.empty();
        }
        Transform transform = ref.getTransform();
        if (transform == null) {
            return Optional.empty();
        }
        Vector3d position = transform.getPosition();
        Vector3f rotation = transform.getRotation();
        if (position == null || rotation == null) {
            return Optional.empty();
        }
        World world = Universe.get().getWorld(ref.getWorldUuid());
        String worldName = world != null ? world.getName() : ref.getWorldUuid().toString();
        return Optional.of(new Location(
            worldName,
            position.getX(),
            position.getY(),
            position.getZ(),
            rotation.getYaw(),
            rotation.getPitch()
        ));
    }
}
