package de.xmassmp.fabric;

import de.xmassmp.crystal.Crystal;
import de.xmassmp.crystal.CrystalModels;
import de.xmassmp.crystal.Crystals;
import de.xmassmp.crystal.PristineStage;
import de.xmassmp.item.CustomItem;
import de.xmassmp.item.ItemCatalog;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Kreativ-Tab {@code xmassmp:items} mit allen Items der Mod.
 *
 * <p>Der Tab ist eine Polymer-Item-Gruppe: Clients mit Polymer sehen ihn als echten Tab im
 * Kreativinventar, Vanilla-Clients öffnen ihn im Kreativmodus mit {@code /polymer creative}.
 *
 * <p>Inhalt: jeder Kristall in jedem Tier und jedem Zustand, dazu alle Hilfs-Items. Die Kristalle sind
 * ungebunden (kein Besitzer) – wie bei {@code /xmas give} sind es Anzeige- und Deko-Items, die der
 * Inventar-Abgleich nicht anfasst. Die Energy Bottle steht nur für Operatoren im Tab; ohne Eintrag im
 * Flaschen-Register wäre sie wirkungslos, deshalb registriert {@link XmasSmpFabric} sie beim ersten
 * Rechtsklick eines Admins im Kreativmodus.
 */
final class XmasCreativeTab {

    static final String ID = "items";

    private XmasCreativeTab() {
    }

    static void register() {
        CreativeModeTab tab = PolymerItemGroupUtils.builder()
                .title(Component.translatableWithFallback("itemGroup.xmassmp.items", "Xmas-SMP"))
                .icon(XmasCreativeTab::icon)
                .displayItems((params, output) -> {
                    for (Crystal crystal : Crystals.DEFAULTS) {
                        for (int tier = CrystalModels.MIN_TIER; tier <= CrystalModels.MAX_TIER; tier++) {
                            // Vom makellosen zum zerbrochenen Kristall, wie die Energie sinkt.
                            PristineStage[] stages = PristineStage.values();
                            for (int i = stages.length - 1; i >= 0; i--) {
                                output.accept(FabricItems.create(ItemCatalog.crystal(crystal, tier, stages[i])));
                            }
                        }
                    }
                    for (CustomItem item : CustomItem.values()) {
                        if (item == CustomItem.ENERGY_BOTTLE && !params.hasPermissions()) {
                            continue;
                        }
                        output.accept(FabricItems.create(ItemCatalog.custom(item)));
                    }
                })
                .build();
        PolymerItemGroupUtils.registerPolymerItemGroup(XmasRegistry.id(ID), tab);
    }

    private static ItemStack icon() {
        Crystal first = Crystals.DEFAULTS.getFirst();
        return FabricItems.create(ItemCatalog.crystal(first, CrystalModels.MAX_TIER, PristineStage.PRISTINE));
    }
}
