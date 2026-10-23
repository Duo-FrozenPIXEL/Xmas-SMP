package de.xmassmp.item;

import de.xmassmp.crystal.Crystal;
import de.xmassmp.crystal.CrystalModels;
import de.xmassmp.crystal.PristineStage;
import de.xmassmp.text.LocalizedText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Baut die {@link ItemSpec}s aller Custom-Items. */
public final class ItemCatalog {

    public static final String TYPE_CRYSTAL = "crystal";
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
                LocalizedText.of("lore.xmassmp.energy", "Energie: %s")
                        .withArgs(LocalizedText.literal(energy + "/" + maxEnergy).withColor(accent))
                        .withColor('7')));
        if (soulbound) {
            lore.add(LocalizedText.of("lore.xmassmp.soulbound", "Seelengebunden").withColor('8'));
        }
        return new ItemSpec(
                CrystalModels.BASE_ITEM,
                CrystalModels.customModelData(crystal, tier, stage),
                name,
                lore,
                Map.of(ItemSpec.DATA_TYPE, TYPE_CRYSTAL,
                        "crystal", crystal.id(),
                        "tier", Integer.toString(tier),
                        "stage", stage.id(),
                        "energy", energy + "/" + maxEnergy,
                        "soulbound", Boolean.toString(soulbound),
                        DATA_OWNER, owner.toString()));
    }

    /** Gebundener Kristall, der vom Inventar-Abgleich verwaltet wird. */
    public static boolean isOwnedCrystal(Map<String, String> data) {
        return TYPE_CRYSTAL.equals(data.get(ItemSpec.DATA_TYPE)) && data.containsKey(DATA_OWNER);
    }

    /** Ohne Lore, bis das Verhalten der Items feststeht (todo.md, Abschnitt 2). */
    public static ItemSpec custom(CustomItem item) {
        return new ItemSpec(
                item.baseItem(),
                item.customModelData(),
                item.displayName(),
                List.of(),
                Map.of(ItemSpec.DATA_TYPE, item.id()));
    }
}
