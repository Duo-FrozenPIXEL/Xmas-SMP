package de.xmassmp.item;

import de.xmassmp.crystal.Crystal;
import de.xmassmp.crystal.CrystalModels;
import de.xmassmp.crystal.PristineStage;
import de.xmassmp.text.LocalizedText;

import java.util.List;
import java.util.Map;

/** Baut die {@link ItemSpec}s aller Custom-Items. */
public final class ItemCatalog {

    public static final String TYPE_CRYSTAL = "crystal";

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
