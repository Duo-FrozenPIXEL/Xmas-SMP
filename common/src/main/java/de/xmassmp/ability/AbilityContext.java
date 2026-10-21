package de.xmassmp.ability;

import de.xmassmp.crystal.Crystal;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.player.PlayerData;

/**
 * Alles, was eine Fähigkeit beim Auslösen braucht.
 *
 * @param damageMultiplier Multiplikator aus {@code crystals.<id>.damageMultiplier}
 */
public record AbilityContext(
        PlayerHandle player,
        PlayerData data,
        Crystal crystal,
        EffectEngine effects,
        double damageMultiplier) {
}
