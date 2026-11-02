package de.xmassmp.ability;

import de.xmassmp.crystal.Crystal;
import de.xmassmp.crystal.Crystals;
import de.xmassmp.util.Rgb;
import de.xmassmp.util.Vec3;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderAbilityTest {

    @Test
    void everyCrystalGetsThreeSlots() {
        AbilityRegistry registry = new AbilityRegistry();
        PlaceholderAbility.registerAll(registry, Crystals.DEFAULTS);
        for (Crystal crystal : Crystals.DEFAULTS) {
            for (int slot = 1; slot <= Crystal.SLOTS; slot++) {
                ActiveAbility ability = registry.active(crystal.id(), slot).orElseThrow();
                assertInstanceOf(PlaceholderAbility.class, ability);
                assertEquals(slot == 3 ? 2 : 1, ability.requiredTier());
            }
        }
    }

    @Test
    void realAbilityReplacesPlaceholderOfItsSlot() {
        AbilityRegistry registry = new AbilityRegistry();
        PlaceholderAbility.registerAll(registry, Crystals.DEFAULTS);
        ActiveAbility real = new ActiveAbility() {
            public String id() { return "fire.flame_burst"; }
            public String crystalId() { return "fire"; }
            public int requiredTier() { return 1; }
            public int slot() { return 1; }
            public Duration cooldown() { return Duration.ofSeconds(5); }
            public void activate(AbilityContext context) { }
        };
        registry.register(real);
        assertEquals(real, registry.active("fire", 1).orElseThrow());
        assertTrue(registry.get("fire.placeholder_1").isEmpty());
        assertInstanceOf(PlaceholderAbility.class, registry.active("fire", 2).orElseThrow());

        registry.clearPlaceholders();
        assertFalse(registry.active("fire", 2).isPresent());
        assertEquals(real, registry.active("fire", 1).orElseThrow());
    }

    @Test
    void timelinesBuildWithoutErrors() {
        Rgb c = Crystals.FIRE.color();
        Rgb a = Crystals.FIRE.accentColor();
        assertTrue(PlaceholderAbility.ring(c, a).length() > 0);
        assertTrue(PlaceholderAbility.beam(c, a, Vec3.ZERO).length() > 0);
        assertTrue(PlaceholderAbility.ultimate(c, a).length() > 20);
    }
}
