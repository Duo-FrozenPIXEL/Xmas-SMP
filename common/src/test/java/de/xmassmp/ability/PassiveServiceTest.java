package de.xmassmp.ability;

import de.xmassmp.MutableClock;
import de.xmassmp.TestPlatform;
import de.xmassmp.TestPlatform.FakePlayer;
import de.xmassmp.XmasCore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Passive wirken immer – auch bei 0 Energie –, solange Kristall und Tier passen. */
class PassiveServiceTest {

    @TempDir
    Path dir;

    /** Aufrufe als {@code spieler@stärke}. */
    private final List<String> ticked = new ArrayList<>();
    private TestPlatform platform;
    private XmasCore core;
    private FakePlayer steve;

    private PassiveAbility passive(String id, String crystal, int tier) {
        return new PassiveAbility() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String crystalId() {
                return crystal;
            }

            @Override
            public int requiredTier() {
                return tier;
            }

            @Override
            public void tick(AbilityContext context) {
                ticked.add(id + ":" + context.player().name() + "@" + context.strength());
            }
        };
    }

    @BeforeEach
    void setUp() {
        platform = new TestPlatform(dir);
        core = new XmasCore(platform, new MutableClock(Instant.parse("2026-12-20T12:00:00Z")));
        core.abilities().register(passive("fire.warmth", "fire", 1));
        core.start();
        steve = platform.join("Steve");
        core.players().get(steve.uuid()).setCrystalId("fire");
        core.onPlayerJoin(steve);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            core.tick();
        }
    }

    @Test
    void passiveTicksEvenAtZeroEnergy() {
        core.players().get(steve.uuid()).setEnergy(0);
        ticks(40);
        assertEquals(2, ticked.size(), "Standard-Intervall 20 Ticks: " + ticked);
        assertEquals("fire.warmth:Steve@0.6", ticked.get(0), "bei 0 Energie mit energy.minStrength");
    }

    @Test
    void passiveStrengthFollowsEnergy() {
        core.players().get(steve.uuid()).setEnergy(10);
        ticks(20);
        assertEquals(List.of("fire.warmth:Steve@1.0"), ticked);
    }

    @Test
    void passiveRespectsTierCrystalAliveAndSwitches() {
        core.abilities().register(passive("fire.inferno", "fire", 2));
        ticks(20);
        assertEquals(1, ticked.size(), "Tier-2-Passive noch gesperrt: " + ticked);

        ticked.clear();
        steve.alive = false;
        ticks(20);
        assertTrue(ticked.isEmpty(), "Tote bekommen keine Passive");

        steve.alive = true;
        core.config().get().general.abilitiesEnabled = false;
        ticks(20);
        assertTrue(ticked.isEmpty(), "Fähigkeiten global aus");

        core.config().get().general.abilitiesEnabled = true;
        core.players().get(steve.uuid()).setCrystalId("wind");
        ticks(20);
        assertTrue(ticked.isEmpty(), "anderer Kristall");
    }
}
