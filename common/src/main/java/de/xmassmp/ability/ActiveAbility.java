package de.xmassmp.ability;

/** Aktiv ausgelöste Fähigkeit mit Cooldown. */
public non-sealed interface ActiveAbility extends Ability {

    /** HUD-Slot (1 oder 2), bestimmt Icon und Taste. */
    int slot();

    void activate(AbilityContext context);
}
