package de.xmassmp.ability;

/** Dauerhaft wirkende Fähigkeit. */
public non-sealed interface PassiveAbility extends Ability {

    /** Wie oft {@link #tick(AbilityContext)} aufgerufen wird, in Server-Ticks. */
    default int tickInterval() {
        return 20;
    }

    void tick(AbilityContext context);
}
