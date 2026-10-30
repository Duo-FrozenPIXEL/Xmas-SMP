package de.xmassmp.effect;

import de.xmassmp.platform.WorldRef;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.util.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * bug.md Nr. 8: Ein verzögerter Sound, dessen Sink-Aufruf wirft, wird genau einmal versucht und
 * stört weder spätere Ticks noch andere Effekte.
 */
class DelayedSoundTest {

    private static final WorldRef WORLD = new WorldRef("overworld");
    private static final String BROKEN = "minecraft:does.not.exist";

    private final List<String> calls = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private int brokenAttempts;

    private final EffectSink sink = new EffectSink() {
        @Override
        public void particle(WorldRef world, Vec3 position, ParticleSpec spec, int count) {
            calls.add("p:" + spec.type());
        }

        @Override
        public void sound(WorldRef world, Vec3 position, String sound, float volume, float pitch) {
            if (BROKEN.equals(sound)) {
                brokenAttempts++;
                throw new IllegalArgumentException("Unbekannte Sound-ID: " + sound);
            }
            calls.add("s:" + sound);
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
            errors.add(message);
        }
    };

    private final EffectEngine engine = new EffectEngine(sink, () -> EffectQuality.HIGH, () -> 10_000,
            new Random(1), logger);

    @Test
    void failingDelayedSoundIsTriedOnlyOnce() {
        engine.play(EffectTimeline.builder()
                .sound(0, SoundStack.builder()
                        .layer("minecraft:block.note_block.bell", 1f, 1f, 0f, 0)
                        .layer(BROKEN, 1f, 1f, 0f, 2)
                        .layer("minecraft:block.note_block.chime", 1f, 1f, 0f, 3)
                        .build())
                .during(0, 8, (c, p, f) -> c.particle(ParticleSpec.of("minecraft:flame"), Vec3.ZERO))
                .build(), WORLD, Vec3.ZERO);

        for (int i = 0; i < 20; i++) {
            engine.tick(); // darf nie werfen
        }

        assertEquals(1, brokenAttempts, "fehlerhafter Sound genau einmal versucht");
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains(BROKEN), errors.get(0));
        assertTrue(calls.contains("s:minecraft:block.note_block.bell"), "sofortiger Sound gespielt");
        assertTrue(calls.contains("s:minecraft:block.note_block.chime"), "späterer Sound trotzdem gespielt");
        assertEquals(8, calls.stream().filter(c -> c.equals("p:minecraft:flame")).count(),
                "Effekt läuft trotz Sound-Fehler vollständig");
        assertEquals(0, engine.activeCount());
    }

    @Test
    void otherEffectsKeepRunning() {
        engine.play(EffectTimeline.builder()
                .sound(0, SoundStack.builder().layer(BROKEN, 1f, 1f, 0f, 1).build())
                .build(), WORLD, Vec3.ZERO);
        engine.tick();
        engine.play(EffectTimeline.builder()
                .during(0, 5, (c, p, f) -> c.particle(ParticleSpec.of("minecraft:snowflake"), Vec3.ZERO))
                .sound(4, SoundStack.builder().layer("minecraft:entity.player.levelup", 1f, 1f, 0f, 2).build())
                .build(), WORLD, Vec3.ZERO);

        for (int i = 0; i < 15; i++) {
            engine.tick();
        }

        assertEquals(1, brokenAttempts);
        assertEquals(5, calls.stream().filter(c -> c.equals("p:minecraft:snowflake")).count());
        assertTrue(calls.contains("s:minecraft:entity.player.levelup"), calls.toString());
        assertEquals(1, errors.size(), errors.toString());
    }

    @Test
    void severalFailingSoundsInSameTickAreAllRemoved() {
        engine.play(EffectTimeline.builder()
                .sound(0, SoundStack.builder()
                        .layer(BROKEN, 1f, 1f, 0f, 2)
                        .layer(BROKEN, 1f, 1f, 0f, 2)
                        .layer("minecraft:block.note_block.harp", 1f, 1f, 0f, 2)
                        .build())
                .build(), WORLD, Vec3.ZERO);
        for (int i = 0; i < 10; i++) {
            engine.tick();
        }
        assertEquals(2, brokenAttempts, "jede fehlerhafte Ebene genau einmal");
        assertEquals(List.of("s:minecraft:block.note_block.harp"), calls);
        assertEquals(2, errors.size());
    }
}
