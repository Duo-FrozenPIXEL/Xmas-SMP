package de.xmassmp.energy;

import de.xmassmp.MutableClock;
import de.xmassmp.TestPlatform;
import de.xmassmp.TestPlatform.FakePlayer;
import de.xmassmp.XmasCore;
import de.xmassmp.effect.DisplayHandle;
import de.xmassmp.effect.DisplaySpec;
import de.xmassmp.effect.DisplayTransform;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.effect.EffectHandle;
import de.xmassmp.effect.EffectQuality;
import de.xmassmp.effect.EffectSink;
import de.xmassmp.effect.EffectTimeline;
import de.xmassmp.effect.ParticleSpec;
import de.xmassmp.platform.WorldRef;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.util.Rgb;
import de.xmassmp.util.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bruch- und Reparatur-Animationen beim Wechsel der Bruchstufe. */
class StageEffectsTest {

    private static final WorldRef WORLD = new WorldRef("overworld");
    private static final Rgb COLOR = Rgb.hex("#FF5500");
    private static final Rgb ACCENT = Rgb.hex("#FFD080");

    @TempDir
    Path dir;

    private final Set<String> particleTypes = new LinkedHashSet<>();
    private final List<String> sounds = new ArrayList<>();
    private final List<String> displayCalls = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private int spawned;
    private int alive;

    private final EffectSink sink = new EffectSink() {
        @Override
        public void particle(WorldRef world, Vec3 position, ParticleSpec spec, int count) {
            particleTypes.add(spec.type());
        }

        @Override
        public void sound(WorldRef world, Vec3 position, String sound, float volume, float pitch) {
            assertTrue(pitch >= 0.5f && pitch <= 2f, "Tonhöhe im gültigen Bereich: " + pitch);
            sounds.add(sound);
        }

        @Override
        public DisplayHandle spawnDisplay(WorldRef world, Vec3 position, DisplaySpec spec) {
            spawned++;
            alive++;
            displayCalls.add("spawn " + spec.id());
            return new DisplayHandle() {
                private boolean removed;

                @Override
                public void moveTo(Vec3 to, int ticks) {
                    if (removed) {
                        throw new AssertionError("Bewegung nach dem Entfernen");
                    }
                    displayCalls.add("move/" + ticks);
                }

                @Override
                public void transform(DisplayTransform transform, int ticks) {
                    if (removed) {
                        throw new AssertionError("Transformation nach dem Entfernen");
                    }
                    displayCalls.add("transform/" + ticks);
                }

                @Override
                public void remove() {
                    if (removed) {
                        throw new AssertionError("doppelt entfernt");
                    }
                    removed = true;
                    alive--;
                    displayCalls.add("remove");
                }

                @Override
                public boolean isRemoved() {
                    return removed;
                }
            };
        }
    };

    private final XmasLogger logger = new XmasLogger() {
        @Override
        public void info(String message) {
        }

        @Override
        public void warn(String message) {
        }

        @Override
        public void error(String message, Throwable error) {
            errors.add(message + ": " + error);
        }
    };

    private final EffectEngine engine = new EffectEngine(sink, () -> EffectQuality.HIGH, () -> 10_000,
            () -> 256, new Random(1), logger);

    /** Spielt die Timeline bis zum Ende ab und liefert die Zahl der Ticks. */
    private int playToEnd(EffectTimeline timeline) {
        EffectHandle handle = engine.play(timeline, WORLD, new Vec3(0, 64, 0));
        int ticks = 0;
        while (!handle.isDone() && ticks < 200) {
            engine.tick();
            ticks++;
        }
        assertTrue(handle.isDone(), "Effekt endet");
        // Verzögerte Sounds abspielen lassen.
        for (int i = 0; i < 10; i++) {
            engine.tick();
        }
        return ticks;
    }

    @Test
    void shatterPlaysAndCleansUp() {
        EffectTimeline timeline = StageEffects.shatter(COLOR, ACCENT, false);
        assertEquals(35, timeline.length());
        playToEnd(timeline);

        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(particleTypes.contains("minecraft:dust"), particleTypes.toString());
        assertTrue(particleTypes.contains("minecraft:electric_spark"), particleTypes.toString());
        assertTrue(particleTypes.contains("minecraft:dust_color_transition"), particleTypes.toString());
        assertTrue(particleTypes.contains("minecraft:crit"), particleTypes.toString());
        assertFalse(particleTypes.contains("minecraft:large_smoke"), "Rauch nur beim völligen Zerbrechen");

        assertEquals(6, spawned, "sechs Scherben");
        assertEquals(0, alive, "alle Scherben entfernt");
        assertEquals(0, engine.activeDisplays());
        assertEquals(6, displayCalls.stream().filter(c -> c.startsWith("move/")).count());
        assertEquals(6, displayCalls.stream().filter(c -> c.equals("remove")).count());

        assertEquals(List.of("minecraft:block.amethyst_block.break", "minecraft:block.amethyst_block.resonate"),
                sounds);
        assertEquals(0, engine.activeCount());
    }

    @Test
    void brokenShatterAddsSmokeAndEcho() {
        playToEnd(StageEffects.shatter(COLOR, ACCENT, true));
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(particleTypes.contains("minecraft:large_smoke"), particleTypes.toString());
        assertTrue(sounds.contains("minecraft:block.respawn_anchor.deplete"), sounds.toString());
        assertEquals(3, sounds.size(), sounds.toString());
        assertEquals(0, alive);
    }

    @Test
    void restorePlaysAndCleansUp() {
        EffectTimeline timeline = StageEffects.restore(COLOR, ACCENT, false);
        assertEquals(34, timeline.length());
        playToEnd(timeline);

        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(particleTypes.contains("minecraft:dust_color_transition"), particleTypes.toString());
        assertTrue(particleTypes.contains("minecraft:end_rod"), particleTypes.toString());
        assertTrue(particleTypes.contains("minecraft:dust"), particleTypes.toString());
        assertTrue(particleTypes.contains("minecraft:wax_off"), particleTypes.toString());

        assertEquals(1, spawned, "eine Scherbe setzt sich zusammen");
        assertEquals(0, alive, "Scherbe am Ende entfernt");
        assertEquals(0, engine.activeDisplays());
        assertEquals(2, displayCalls.stream().filter(c -> c.startsWith("transform/")).count(), displayCalls.toString());
        assertEquals(List.of("minecraft:block.amethyst_block.resonate", "minecraft:block.amethyst_block.chime"), sounds);
    }

    @Test
    void pristineRestorePlaysLevelUp() {
        playToEnd(StageEffects.restore(COLOR, ACCENT, true));
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(sounds.contains("minecraft:entity.player.levelup"), sounds.toString());
        assertFalse(sounds.contains("minecraft:block.amethyst_block.chime"), sounds.toString());
        assertEquals(0, alive);
    }

    @Test
    void severalEffectsAtOnceRemoveAllDisplays() {
        engine.play(StageEffects.shatter(COLOR, ACCENT, true), WORLD, Vec3.ZERO);
        engine.play(StageEffects.restore(ACCENT, COLOR, true), WORLD, Vec3.ZERO);
        for (int i = 0; i < 15; i++) {
            engine.tick();
        }
        assertEquals(7, alive, "6 Scherben + 1 Reparatur-Scherbe gleichzeitig");
        for (int i = 0; i < 40; i++) {
            engine.tick();
        }
        assertEquals(0, alive);
        assertEquals(0, engine.activeCount());
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void cancelRemovesShards() {
        EffectHandle handle = engine.play(StageEffects.shatter(COLOR, ACCENT, false), WORLD, Vec3.ZERO);
        for (int i = 0; i < 15; i++) {
            engine.tick();
        }
        assertEquals(6, alive);
        handle.cancel();
        engine.tick();
        assertEquals(0, alive);
        assertTrue(handle.isDone());
    }

    // ------------------------------------------------------------------ über den Kern

    @Test
    void stageChangeThroughCoreEmitsParticles() {
        TestPlatform platform = new TestPlatform(dir);
        XmasCore core = new XmasCore(platform, new MutableClock(Instant.parse("2026-12-20T12:00:00Z")));
        core.start();
        FakePlayer steve = platform.join("Steve");
        FakePlayer alex = platform.join("Alex");
        core.players().get(steve.uuid()).setCrystalId("fire");
        core.players().get(alex.uuid()).setCrystalId("wind");
        core.onPlayerJoin(steve);
        core.onPlayerJoin(alex);

        // Bruch: SCRATCHED -> DAMAGED.
        core.players().get(alex.uuid()).setEnergy(3);
        core.onPlayerDeath(alex, null);
        for (int i = 0; i < 5; i++) {
            core.tick();
        }
        assertTrue(platform.effects.contains("p:minecraft:electric_spark"), platform.effects.toString());
        assertTrue(platform.effects.contains("p:minecraft:dust"), platform.effects.toString());
        for (int i = 0; i < 45; i++) {
            core.tick();
        }
        assertTrue(platform.effects.contains("p:minecraft:crit"));
        assertTrue(platform.effects.contains("s:minecraft:block.amethyst_block.break"));
        assertEquals(0, core.effects().activeCount(), "Animation beendet");
        assertEquals(0, core.effects().activeDisplays(), "Scherben aufgeräumt");

        // Reparatur: DAMAGED (2) -> SCRATCHED (3) durch einen Kill.
        platform.effects.clear();
        core.onPlayerDeath(steve, alex);
        assertEquals(3, core.players().get(alex.uuid()).energy());
        for (int i = 0; i < 50; i++) {
            core.tick();
        }
        assertTrue(platform.effects.contains("p:minecraft:end_rod"), platform.effects.toString());
        assertTrue(platform.effects.contains("s:minecraft:block.amethyst_block.resonate"));
        assertEquals(0, core.effects().activeDisplays());
        assertTrue(platform.errors.isEmpty(), platform.errors.toString());
    }
}
