package de.xmassmp.fabric;

import de.xmassmp.XmasCore;
import de.xmassmp.item.CustomItem;
import de.xmassmp.util.BlockPos;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import xyz.nucleoid.packettweaker.PacketContext;

/**
 * Server-Item für Kristalle und Hilfs-Items. Der Client sieht {@code base} mit dem Modell aus
 * {@code custom_model_data}; eine eigene Vanilla-Funktion hat das Item nicht.
 *
 * <p>Rechtsklicks laufen über {@code UseItemCallback} in den Kern. Nur das Repair Kit reagiert auf
 * Blöcke: Es stellt das Totem auf.
 */
final class XmasItem extends SimplePolymerItem {

    private final String type;

    XmasItem(Properties properties, Item base, String type) {
        super(properties, base);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        XmasCore core = XmasSmpFabric.core();
        if (!CustomItem.REPAIR_KIT.id().equals(type) || core == null
                || !(context.getPlayer() instanceof ServerPlayer player) || context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        BlockPlaceContext place = new BlockPlaceContext(context);
        net.minecraft.core.BlockPos target = place.getClickedPos();
        boolean handled = core.totem().tryPlace(XmasSmpFabric.handle(player), FabricPlatform.worldRef(player.level()),
                new BlockPos(target.getX(), target.getY(), target.getZ()));
        return handled ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
    }

    @Override
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context) {
        // Polymer übernimmt den Verzauberungs-Schimmer nicht von selbst (Kristall bei voller Energie).
        Boolean glint = stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        if (glint != null) {
            out.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, glint);
        }
    }
}
