package de.xmassmp.fabric;

import de.xmassmp.XmasCore;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockState;
import xyz.nucleoid.packettweaker.PacketContext;

/**
 * Totem des Repair Kits. Clients sehen einen aktiven, unheilvollen Tresor ({@code minecraft:vault}).
 *
 * <p>Abbauen läuft über Polymer auf dem Server (der Client bekommt Abbau-Lähmung, damit er nicht
 * selbst „fertig abbaut“). Der Fortschritt pro Tick ist fest {@code 1 / totem.breakSeconds·20} –
 * Werkzeug, Effizienz, Eile und Abbaulähmung spielen keine Rolle. Loslassen setzt ihn zurück.
 */
final class TotemBlock extends Block implements PolymerBlock {

    TotemBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.VAULT.defaultBlockState()
                .setValue(VaultBlock.OMINOUS, true)
                .setValue(VaultBlock.STATE, VaultState.ACTIVE);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        XmasCore core = XmasSmpFabric.core();
        return core == null ? 0f : 1f / Math.max(1, core.totem().breakTicks());
    }
}
