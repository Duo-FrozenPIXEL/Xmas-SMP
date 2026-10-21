package de.xmassmp.fabric;

import net.minecraft.resources.Identifier;

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
}
