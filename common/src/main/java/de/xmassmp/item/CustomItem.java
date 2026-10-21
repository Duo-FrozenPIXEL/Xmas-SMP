package de.xmassmp.item;

import de.xmassmp.text.LocalizedText;

import java.util.Locale;
import java.util.Optional;

/** Hilfs-Items aus dem Original. Basis-Item und CMD laut docs/resourcepack.md. */
public enum CustomItem {
    REVIVAL_TOME("minecraft:enchanted_book", 1, "Wiederbelebungs-Foliant", 'd'),
    UPGRADER("minecraft:enchanted_book", 3001, "Upgrader", 'e'),
    ENERGY_BOTTLE("minecraft:ghast_tear", 4001, "Energieflasche", 'b'),
    GEM_TRADER("minecraft:emerald", 4002, "Kristall-Händler", 'a'),
    REPAIR_KIT("minecraft:beacon", 4003, "Reparatur-Set", 'c'),
    GEM_FRAGMENT("minecraft:prismarine_shard", 4004, "Kristall-Splitter", '3');

    private final String baseItem;
    private final int customModelData;
    private final String nameFallback;
    private final char color;

    CustomItem(String baseItem, int customModelData, String nameFallback, char color) {
        this.baseItem = baseItem;
        this.customModelData = customModelData;
        this.nameFallback = nameFallback;
        this.color = color;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String baseItem() {
        return baseItem;
    }

    public int customModelData() {
        return customModelData;
    }

    public LocalizedText displayName() {
        return LocalizedText.of("item.xmassmp." + id(), nameFallback).withColor(color);
    }

    public static Optional<CustomItem> byId(String id) {
        for (CustomItem item : values()) {
            if (item.id().equals(id)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }
}
