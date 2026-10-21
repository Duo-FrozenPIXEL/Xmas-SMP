package de.xmassmp.ability;

import de.xmassmp.player.PlayerData;

import java.time.Duration;

/**
 * Eine Kristall-Fähigkeit. Konkrete Kräfte implementieren {@link ActiveAbility} oder
 * {@link PassiveAbility}; der Entwurf steht in todo.md, Abschnitt 5.
 */
public sealed interface Ability permits ActiveAbility, PassiveAbility {

    /** Eindeutige ID, z. B. {@code fire.flame_burst}. Dient auch als Cooldown-Schlüssel. */
    String id();

    /** ID des Kristalls, zu dem die Fähigkeit gehört. */
    String crystalId();

    /** Mindest-Tier, ab dem die Fähigkeit freigeschaltet ist. */
    int requiredTier();

    default boolean isUnlocked(PlayerData data) {
        return data.tier() >= requiredTier();
    }

    /** Basis-Cooldown vor Multiplikatoren aus Config und Events. Passive: {@link Duration#ZERO}. */
    default Duration cooldown() {
        return Duration.ZERO;
    }
}
