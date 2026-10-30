package de.xmassmp.fabric;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Einzige Stelle, die Minecraft-IDs aus Strings baut.
 *
 * <p>(V) Ab 1.21.11 heißt {@code ResourceLocation} in den Mojang-Namen {@code Identifier}. Falls
 * die Zielversion das anders handhabt, muss nur diese Klasse angepasst werden.
 */
final class McIds {

    private McIds() {
    }

    static Identifier parse(String id) {
        return Identifier.parse(id);
    }

    /** ID eines Schlüssels, z. B. {@code minecraft:overworld} für eine Dimension. (V) ab 1.21.11 {@code identifier()}. */
    static String id(ResourceKey<?> key) {
        return key.identifier().toString();
    }
}
