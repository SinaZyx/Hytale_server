package com.kingc.hytale.duels.kit;

import com.kingc.hytale.duels.api.ItemStack;

import java.util.List;
import java.util.Map;

public record KitDefinition(
    String id,
    String displayName,
    String iconItem,
    ItemStack helmet,
    ItemStack chestplate,
    ItemStack leggings,
    ItemStack boots,
    List<ItemStack> items,
    Map<String, EffectEntry> effects
) {
    public record EffectEntry(String effectType, int amplifier, int durationTicks) {}

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private String displayName;
        private String iconItem = "hytale:iron_sword";
        private ItemStack helmet;
        private ItemStack chestplate;
        private ItemStack leggings;
        private ItemStack boots;
        private List<ItemStack> items = List.of();
        private Map<String, EffectEntry> effects = Map.of();

        private Builder(String id) {
            this.id = id;
            this.displayName = id;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder iconItem(String iconItem) {
            this.iconItem = iconItem;
            return this;
        }

        public Builder armor(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
            this.helmet = helmet;
            this.chestplate = chestplate;
            this.leggings = leggings;
            this.boots = boots;
            return this;
        }

        public Builder items(List<ItemStack> items) {
            this.items = List.copyOf(items);
            return this;
        }

        public Builder effects(Map<String, EffectEntry> effects) {
            this.effects = Map.copyOf(effects);
            return this;
        }

        public KitDefinition build() {
            return new KitDefinition(id, displayName, iconItem, helmet, chestplate, leggings, boots, items, effects);
        }
    }
}
