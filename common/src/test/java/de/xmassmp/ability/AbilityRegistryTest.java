package de.xmassmp.ability;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilityRegistryTest {

    private static ActiveAbility active(String id, int slot) {
        return new ActiveAbility() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String crystalId() {
                return "fire";
            }

            @Override
            public int requiredTier() {
                return 1;
            }

            @Override
            public Duration cooldown() {
                return Duration.ofSeconds(10);
            }

            @Override
            public int slot() {
                return slot;
            }

            @Override
            public void activate(AbilityContext context) {
            }
        };
    }

    @Test
    void invalidSlotThrowsAndIsNotRegistered() {
        AbilityRegistry registry = new AbilityRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.register(active("fire.spark", 3)));
        assertFalse(registry.get("fire.spark").isPresent());
        assertTrue(registry.forCrystal("fire").isEmpty());
        assertTrue(registry.active("fire", 3).isEmpty());
    }

    @Test
    void correctedRetryWithSameIdSucceeds() {
        AbilityRegistry registry = new AbilityRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.register(active("fire.spark", 3)));

        ActiveAbility fixed = active("fire.spark", 1);
        registry.register(fixed);
        assertEquals(fixed, registry.get("fire.spark").orElseThrow());
        assertEquals(1, registry.forCrystal("fire").size());
        assertEquals(fixed, registry.active("fire", 1).orElseThrow());
    }
}
