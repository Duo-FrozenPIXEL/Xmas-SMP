package de.xmassmp.item;

import de.xmassmp.crystal.Crystal;
import de.xmassmp.crystal.CrystalModels;
import de.xmassmp.crystal.PristineStage;
import de.xmassmp.energy.EnergyLevel;
import de.xmassmp.text.LocalizedText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Baut die {@link ItemSpec}s aller Custom-Items. */
public final class ItemCatalog {

    public static final String NAMESPACE = "xmassmp";
    public static final String TYPE_CRYSTAL = "crystal";
    /** Server-Item-ID der Kristalle. */
    public static final String CRYSTAL_ITEM = NAMESPACE + ":" + TYPE_CRYSTAL;
    /** Eindeutige ID einer Energy Bottle, siehe {@link de.xmassmp.bottle.BottleRegistry}. */
    public static final String DATA_BOTTLE = "bottle";
    /** {@code "true"}: Verzauberungs-Schimmer (Kristall bei maximaler Energie). */
    public static final String DATA_GLINT = "glint";
    /** UUID des Besitzers. Nur gebundene Kristalle haben sie; nur diese gleicht der Kern ab. */
    public static final String DATA_OWNER = "owner";

    private ItemCatalog() {
    }

    public static ItemSpec crystal(Crystal crystal, int tier, PristineStage stage) {
        LocalizedText name = LocalizedText.of("item.xmassmp.crystal", "%s-Kristall")
                .withArgs(crystal.displayName())
                .withColor(crystal.chatColor());
        List<LocalizedText> lore = List.of(
                LocalizedText.of("lore.xmassmp.tier", "Tier %s")
                        .withArgs(LocalizedText.literal(Integer.toString(tier)))
                        .withColor('7'),
                LocalizedText.of("lore.xmassmp.stage", "Zustand: %s")
                        .withArgs(stage.displayName())
                        .withColor('7'),
                LocalizedText.of("lore.xmassmp.soulbound", "Seelengebunden").withColor('8'));
        return new ItemSpec(
                CrystalModels.BASE_ITEM,
                CrystalModels.customModelData(crystal, tier, stage),
                name,
                lore,
                Map.of(ItemSpec.DATA_TYPE, TYPE_CRYSTAL,
                        "crystal", crystal.id(),
                        "tier", Integer.toString(tier),
                        "stage", stage.id()));
    }

    /**
     * Der an einen Spieler gebundene Kristall. Alle angezeigten Werte stehen auch in den Daten,
     * damit der Inventar-Abgleich ein veraltetes Item am Unterschied erkennt.
     */
    public static ItemSpec ownedCrystal(Crystal crystal, int tier, PristineStage stage, int energy, int maxEnergy,
                                        UUID owner, boolean soulbound) {
        char accent = crystal.chatColor();
        LocalizedText name = LocalizedText.of("item.xmassmp.crystal", "%s-Kristall")
                .withArgs(crystal.displayName())
                .withColor(accent);
        List<LocalizedText> lore = new ArrayList<>(List.of(
                LocalizedText.of("lore.xmassmp.tier", "Tier %s")
                        .withArgs(LocalizedText.literal(Integer.toString(tier)).withColor(accent))
                        .withColor('7'),
                LocalizedText.of("lore.xmassmp.stage", "Zustand: %s")
                        .withArgs(stage.displayName())
                        .withColor('7'),
                LocalizedText.of("lore.xmassmp.energy", "Energie: %s (%s)")
                        .withArgs(LocalizedText.literal(energy + "/" + maxEnergy).withColor(accent),
                                EnergyLevel.name(energy))
                        .withColor('7')));
        if (soulbound) {
            lore.add(LocalizedText.of("lore.xmassmp.soulbound", "Seelengebunden").withColor('8'));
        }
        Map<String, String> data = new HashMap<>(Map.of(ItemSpec.DATA_TYPE, TYPE_CRYSTAL,
                "crystal", crystal.id(),
                "tier", Integer.toString(tier),
                "stage", stage.id(),
                "energy", energy + "/" + maxEnergy,
                "soulbound", Boolean.toString(soulbound),
                DATA_OWNER, owner.toString()));
        if (energy >= maxEnergy) {
            data.put(DATA_GLINT, "true");
        }
        return new ItemSpec(
                CrystalModels.BASE_ITEM,
                CrystalModels.customModelData(crystal, tier, stage),
                name,
                lore,
                data);
    }

    /** Gebundener Kristall, der vom Inventar-Abgleich verwaltet wird. */
    public static boolean isOwnedCrystal(Map<String, String> data) {
        return TYPE_CRYSTAL.equals(data.get(ItemSpec.DATA_TYPE)) && data.containsKey(DATA_OWNER);
    }

    /**
     * Ein Hilfs-Item mit erklärender Lore. Energy Bottles nur über {@link #energyBottle(UUID)}, sonst
     * fehlt ihnen der Eintrag im Flaschen-Register und sie wirken nicht.
     */
    public static ItemSpec custom(CustomItem item) {
        return new ItemSpec(
                item.baseItem(),
                item.customModelData(),
                item.displayName(),
                lore(item),
                Map.of(ItemSpec.DATA_TYPE, item.id()));
    }

    /** Energy Bottle mit eindeutiger ID aus dem Flaschen-Register. */
    public static ItemSpec energyBottle(UUID id) {
        CustomItem item = CustomItem.ENERGY_BOTTLE;
        return new ItemSpec(
                item.baseItem(),
                item.customModelData(),
                item.displayName(),
                lore(item),
                Map.of(ItemSpec.DATA_TYPE, item.id(), DATA_BOTTLE, id.toString()));
    }

    /** Server-Item-ID zu einem Item-Typ aus {@code custom_data}, z. B. {@code xmassmp:upgrader}. */
    public static String itemId(String type) {
        return NAMESPACE + ":" + type;
    }

    private static List<LocalizedText> lore(CustomItem item) {
        return switch (item) {
            case UPGRADER -> List.of(
                    line("lore.xmassmp.upgrader.1", "Rechtsklick: Kristall auf Tier 2 heben", '7'),
                    line("lore.xmassmp.upgrader.2", "Schaltet die Ultimate frei", '8'));
            case ENERGY_BOTTLE -> List.of(
                    line("lore.xmassmp.energy_bottle.1", "Rechtsklick: +1 Energie", '7'),
                    line("lore.xmassmp.energy_bottle.2", "Werkbank: 9 Kristall-Splitter", '8'));
            case GEM_TRADER -> List.of(
                    line("lore.xmassmp.gem_trader.1", "Rechtsklick: Kristall frei wählen", '7'),
                    line("lore.xmassmp.gem_trader.2", "Tier und Energie bleiben erhalten", '8'));
            case REPAIR_KIT -> List.of(
                    line("lore.xmassmp.repair_kit.1", "Auf den Boden stellen: Totem für 10 Minuten", '7'),
                    line("lore.xmassmp.repair_kit.2", "Wer bis zum Ende im Kreis bleibt, bekommt 5 Energie", '8'),
                    line("lore.xmassmp.repair_kit.3", "Die Koordinaten sieht jeder!", 'c'));
            case GEM_FRAGMENT -> List.of(
                    line("lore.xmassmp.gem_fragment.1", "Zutat für Upgrader, Händler und Repair Kit", '7'));
        };
    }

    private static LocalizedText line(String key, String fallback, char color) {
        return LocalizedText.of(key, fallback).withColor(color);
    }
}
