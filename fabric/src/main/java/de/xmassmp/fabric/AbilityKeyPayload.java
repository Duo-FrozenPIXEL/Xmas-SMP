package de.xmassmp.fabric;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → Server: „Taste für Fähigkeit {@code slot} gedrückt“.
 *
 * <p>Muss exakt zur Definition im Client-Mod (client/) passen: ID {@code xmassmp:ability}, Inhalt ein VAR_INT.
 * Der Server prüft Cooldown, Energie und Tier selbst – das Paket ist nur ein Wunsch.
 */
record AbilityKeyPayload(int slot) implements CustomPacketPayload {

    public static final int PRIMARY = 1;
    public static final int SECONDARY = 2;
    public static final int ULTIMATE = 3;

    public static final Type<AbilityKeyPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("xmassmp", "ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityKeyPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, AbilityKeyPayload::slot, AbilityKeyPayload::new);

    @Override
    public Type<AbilityKeyPayload> type() {
        return TYPE;
    }
}
