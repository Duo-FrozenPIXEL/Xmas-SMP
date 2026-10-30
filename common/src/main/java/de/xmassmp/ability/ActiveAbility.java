package de.xmassmp.ability;

/** Aktiv ausgelöste Fähigkeit mit Cooldown. */
public non-sealed interface ActiveAbility extends Ability {

    /**
     * HUD-Slot, bestimmt Icon, Taste und Mindest-Energie: 1 = Primär, 2 = Sekundär (beide ab Tier 1),
     * 3 = Ultimate (Tier 2).
     */
    int slot();

    void activate(AbilityContext context);
}
