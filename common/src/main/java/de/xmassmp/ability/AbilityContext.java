package de.xmassmp.ability;

import de.xmassmp.crystal.Crystal;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.player.PlayerData;

/**
 * Alles, was eine Fähigkeit beim Auslösen braucht.
 *
 * @param damageMultiplier Multiplikator aus {@code crystals.<id>.damageMultiplier}
 * @param strength         Faktor aus der Energie für Schaden, Dauer und Radius (über 0 bis 1)
 */
public record AbilityContext(
        PlayerHandle player,
        PlayerData data,
        Crystal crystal,
        EffectEngine effects,
        double damageMultiplier,
        double strength) {

    /** Schaden inklusive Config-Multiplikator und Energie-Stärke. */
    public double scaledDamage(double base) {
        return base * damageMultiplier * strength;
    }
}
