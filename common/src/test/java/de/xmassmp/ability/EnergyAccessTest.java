package de.xmassmp.ability;

import de.xmassmp.MutableClock;
import de.xmassmp.TestPlatform;
import de.xmassmp.TestPlatform.FakePlayer;
import de.xmassmp.XmasCore;
import de.xmassmp.ability.AbilityService.Result;
import de.xmassmp.crystal.Crystals;
import de.xmassmp.player.PlayerData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Drei Slots mit Energie-Zugriff (energy.access 1/3/3), Ultimate ab Tier 2, Steuerung und HUD. */
class EnergyAccessTest {

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-12-20T12:00:00Z"));
    private final List<String> fired = new ArrayList<>();
    private TestPlatform platform;
    private XmasCore core;
    private FakePlayer steve;
    private AbilityControls controls;

    private ActiveAbility ability(String id, int slot, int tier) {
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
                return tier;
            }

            @Override
            public Duration cooldown() {
                return Duration.ofSeconds(1);
            }

            @Override
            public int slot() {
                return slot;
            }

            @Override
            public void activate(AbilityContext context) {
                fired.add(id);
            }
        };
    }

    @BeforeEach
    void setUp() {
        platform = new TestPlatform(dir);
        core = new XmasCore(platform, clock);
        core.abilities().register(ability("fire.primary", 1, 1));
        core.abilities().register(ability("fire.secondary", 2, 1));
        core.abilities().register(ability("fire.ultimate", 3, 2));
        core.start();
        steve = platform.join("Steve");
        core.players().get(steve.uuid()).setCrystalId("fire");
        core.onPlayerJoin(steve);
        // Eigene Instanz für die Rückgabewerte von onKey (der Kern gibt sie nicht weiter)
        controls = new AbilityControls(core.crystals(), core.abilities(), core.abilityService(), core.players(),
                core.config()::get, core.hud(), clock::millis);
    }

    private PlayerData data() {
        return core.players().get(steve.uuid());
    }

    private Result activate(int slot) {
        ActiveAbility ability = core.abilities().active("fire", slot).orElseThrow();
        // Cooldowns sollen hier nie stören
        clock.advance(Duration.ofSeconds(5));
        return core.abilityService().tryActivate(steve, data(), Crystals.FIRE, ability);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            core.tick();
        }
    }

    @Test
    void requiredEnergyComesFromConfig() {
        assertEquals(1, core.energy().requiredEnergy(1));
        assertEquals(3, core.energy().requiredEnergy(2));
        assertEquals(3, core.energy().requiredEnergy(3));
        assertEquals(3, core.abilityService().requiredEnergy(2));
    }

    @Test
    void secondaryNeedsThreeEnergy() {
        data().setEnergy(2);
        assertEquals(Result.LOW_ENERGY, activate(2));
        assertTrue(fired.isEmpty());
        data().setEnergy(3);
        assertEquals(Result.ACTIVATED, activate(2));
        assertEquals(List.of("fire.secondary"), fired);
    }

    @Test
    void primaryWorksAtOneEnergyButNotAtZero() {
        data().setEnergy(1);
        assertEquals(Result.ACTIVATED, activate(1));
        data().setEnergy(0);
        assertEquals(Result.BROKEN, activate(1), "zerbrochener Kristall: alle aktiven Kräfte ruhen");
        assertEquals(List.of("fire.primary"), fired);
    }

    @Test
    void ultimateNeedsTierTwoAndThreeEnergy() {
        data().setEnergy(10);
        assertEquals(Result.LOCKED, activate(3), "Tier 1");
        data().setTier(2);
        data().setEnergy(2);
        assertEquals(Result.LOW_ENERGY, activate(3));
        data().setEnergy(3);
        assertEquals(Result.ACTIVATED, activate(3));
        assertEquals(List.of("fire.ultimate"), fired);
    }

    @Test
    void lowEnergyFeedbackViaSwapHands() {
        data().setEnergy(2);
        steve.sneaking = true;
        assertTrue(core.onSwapHands(steve));
        assertTrue(fired.isEmpty());
        assertEquals("§cDie Sekundärkraft braucht mindestens 3 Energie.", steve.lastActionBar());
        steve.sneaking = false;
        assertTrue(core.onSwapHands(steve));
        assertEquals(List.of("fire.primary"), fired);
    }

    @Test
    void ultimateViaSneakRightClickWithOwnCrystal() {
        data().setTier(2);
        core.crystalService().sync(steve.uuid());
        var crystal = steve.crystals().get(0);
        steve.hold(crystal, 1);

        assertFalse(core.onUseItem(steve, steve.mainHandData()), "ohne Schleichen normales Verhalten");
        steve.sneaking = true;
        assertTrue(core.onUseItem(steve, steve.mainHandData()));
        assertEquals(List.of("fire.ultimate"), fired);

        // Fremder Kristall löst nichts aus
        FakePlayer alex = platform.join("Alex");
        core.players().get(alex.uuid()).setCrystalId("fire");
        core.players().get(alex.uuid()).setTier(2);
        core.onPlayerJoin(alex);
        alex.sneaking = true;
        alex.hold(crystal, 1);
        assertFalse(core.onUseItem(alex, alex.mainHandData()));
        assertEquals(1, fired.size());
    }

    @Test
    void ultimateLockedFeedbackAtTierOne() {
        steve.sneaking = true;
        steve.hold(steve.crystals().get(0), 1);
        assertTrue(core.onUseItem(steve, steve.mainHandData()));
        assertTrue(fired.isEmpty());
        assertTrue(steve.lastActionBar().contains("Tier 2"), steve.lastActionBar());
    }

    @Test
    void onKeyRejectsInvalidSlots() {
        assertFalse(controls.onKey(steve, 0));
        assertFalse(controls.onKey(steve, 4));
        assertFalse(controls.onKey(steve, -1));
        assertTrue(fired.isEmpty());

        assertTrue(controls.onKey(steve, 1));
        assertEquals(List.of("fire.primary"), fired);
    }

    @Test
    void onKeyChecksEnergyAndConfig() {
        data().setEnergy(2);
        assertTrue(controls.onKey(steve, 2), "gültiger Slot wird angenommen …");
        assertTrue(fired.isEmpty(), "… aber ohne genug Energie nicht ausgelöst");

        core.config().get().controls.clientKeys = false;
        assertFalse(controls.onKey(steve, 1), "Client-Tasten abgeschaltet");

        core.config().get().controls.clientKeys = true;
        data().setCrystalId(null);
        assertFalse(controls.onKey(steve, 1), "ohne Kristall");
        assertTrue(fired.isEmpty());
    }

    @Test
    void coreForwardsAbilityKeys() {
        core.onAbilityKey(steve, 1);
        assertEquals(List.of("fire.primary"), fired);
        core.onAbilityKey(steve, 7); // ungültig, darf nichts tun und nicht werfen
        assertEquals(1, fired.size());
        assertTrue(platform.errors.isEmpty(), platform.errors.toString());
    }

    @Test
    void hudShowsRequiredEnergyWhenTooLow() {
        data().setEnergy(2);
        data().setTier(2);
        ticks(10);
        String hud = steve.lastActionBar();
        assertTrue(hud.contains("§f" + Crystals.FIRE.hudIcon(1) + " §a✔"), "Primär bereit: " + hud);
        assertTrue(hud.contains("§8" + Crystals.FIRE.hudIcon(2) + " §c✦3"), "Sekundär braucht 3: " + hud);
        assertTrue(hud.contains("§8" + Crystals.FIRE.hudIcon(3) + " §c✦3"), "Ultimate braucht 3: " + hud);

        data().setEnergy(3);
        ticks(10);
        assertFalse(steve.lastActionBar().contains("✦3"), steve.lastActionBar());
    }

    @Test
    void hudShowsTierBeforeEnergyForLockedUltimate() {
        data().setEnergy(2);
        ticks(10);
        String hud = steve.lastActionBar();
        assertTrue(hud.contains("§8" + Crystals.FIRE.hudIcon(3) + " T2"), hud);
    }
}
