package de.xmassmp.fabric.mixin;

import de.xmassmp.fabric.XmasSmpFabric;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gelber Name in der Tab-Liste für Tier-2-Spieler; ein Name von anderen Mods bleibt unangetastet. */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {

    @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
    private void xmassmp$tier2TabName(CallbackInfoReturnable<Component> cir) {
        if (cir.getReturnValue() == null) {
            Component name = XmasSmpFabric.tabListName((ServerPlayer) (Object) this);
            if (name != null) {
                cir.setReturnValue(name);
            }
        }
    }
}
