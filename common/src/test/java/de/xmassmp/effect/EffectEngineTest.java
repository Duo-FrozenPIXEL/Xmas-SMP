package de.xmassmp.effect;

import de.xmassmp.platform.WorldRef;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.util.Rgb;
import de.xmassmp.util.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectEngineTest {

    private static final WorldRef WORLD = new WorldRef("overworld");

    private final List<String> calls = new ArrayList<>();
    private final EffectSink sink = new EffectSink() {
        @Override
        public void particle(WorldRef world, Vec3 position, ParticleSpec spec, int count) {
            calls.add("p:" + spec.type() + "x" + count + "@" + position.x());
        }

        @Override
        public void sound(WorldRef world, Vec3 position, String sound, float volume, float pitch) {
            calls.add("s:" + sound);
        }
    };

    private EffectQuality quality = EffectQuality.HIGH;
    private int budget = 10_000;
    private final EffectEngine engine = new EffectEngine(sink, () -> quality, () -> budget, new Random(1),
            XmasLogger.NOOP);

    @Test
    void phasesRunSequentiallyWithProgress() {
        List<String> trace = new ArrayList<>();
        EffectTimeline timeline = EffectTimeline.builder()
                .phase(3, (c, p, f) -> trace.add("charge " + f + " " + p))
                .phase(1, (c, p, f) -> trace.add("release"))
                .phase(2, Easing.LINEAR, (c, p, f) -> trace.add("after " + p))
                .build();
        assertEquals(6, timeline.length());

        EffectHandle handle = engine.play(timeline, WORLD, Vec3.ZERO);
        for (int i = 0; i < 7; i++) {
            engine.tick();
        }
        assertEquals(List.of("charge 0 0.0", "charge 1 0.5", "charge 2 1.0", "release", "after 0.0", "after 1.0"), trace);
        assertTrue(handle.isDone());
        assertEquals(0, engine.activeCount());
    }

    @Test
    void lowQualityThinsShapes() {
        quality = EffectQuality.LOW;
        engine.play(EffectTimeline.builder()
                .at(0, c -> c.particles(ParticleSpec.of("minecraft:flame"), Shapes.ring(1, 100, 0)))
                .build(), WORLD, Vec3.ZERO);
        engine.tick();
        assertEquals(35, calls.size());
    }

    @Test
    void particleBudgetDropsExcess() {
        budget = 100;
        engine.play(EffectTimeline.builder()
                .at(0, c -> c.particles(ParticleSpec.dust(Rgb.hex("#FF0000"), 1f).count(10), Shapes.ring(1, 20, 0)))
                .build(), WORLD, Vec3.ZERO);
        engine.tick();
        assertEquals(10, calls.size());
        engine.tick();
        assertEquals(100, engine.droppedParticlesLastTick());
    }

    @Test
    void layeredSoundsRespectDelay() {
        SoundStack stack = SoundStack.builder()
                .layer("minecraft:a", 1f, 1f)
                .layer("minecraft:b", 1f, 1f, 0.1f, 2)
                .build();
        engine.play(EffectTimeline.builder().sound(0, stack).build(), WORLD, Vec3.ZERO);
        engine.tick();
        assertEquals(List.of("s:minecraft:a"), calls);
        engine.tick();
        assertEquals(1, calls.size());
        engine.tick();
        assertEquals(List.of("s:minecraft:a", "s:minecraft:b"), calls);
    }

    @Test
    void anchorIsFollowed() {
        AtomicReference<Vec3> anchor = new AtomicReference<>(new Vec3(1, 0, 0));
        engine.play(EffectTimeline.builder()
                .during(0, 2, (c, p, f) -> c.particle(ParticleSpec.of("minecraft:cloud"), Vec3.ZERO))
                .build(), WORLD, anchor::get);
        engine.tick();
        anchor.set(new Vec3(5, 0, 0));
        engine.tick();
        assertEquals(List.of("p:minecraft:cloudx1@1.0", "p:minecraft:cloudx1@5.0"), calls);
    }

    @Test
    void failingEffectIsRemovedOthersContinue() {
        engine.play(EffectTimeline.builder().during(0, 5, (c, p, f) -> {
            throw new IllegalStateException("kaputt");
        }).build(), WORLD, Vec3.ZERO);
        EffectHandle ok = engine.play(EffectTimeline.builder()
                .during(0, 2, (c, p, f) -> c.particle(ParticleSpec.of("minecraft:flame"), Vec3.ZERO))
                .build(), WORLD, Vec3.ZERO);
        engine.tick();
        engine.tick();
        engine.tick();
        assertEquals(2, calls.size());
        assertTrue(ok.isDone());
    }

    @Test
    void cancelStopsEffect() {
        EffectHandle handle = engine.play(EffectTimeline.builder()
                .during(0, 10, (c, p, f) -> c.particle(ParticleSpec.of("minecraft:flame"), Vec3.ZERO))
                .build(), WORLD, Vec3.ZERO);
        engine.tick();
        handle.cancel();
        engine.tick();
        assertEquals(1, calls.size());
        assertTrue(handle.isDone());
    }

    @Test
    void particleSpecValidation() {
        assertThrows(IllegalArgumentException.class, () -> new ParticleSpec(ParticleSpec.DUST, 1, Vec3.ZERO, 0, null, null, 1));
        assertThrows(IllegalArgumentException.class, () -> ParticleSpec.of("minecraft:flame").count(0));
        assertEquals(4f, ParticleSpec.dust(Rgb.hex("#FFFFFF"), 10f).size());
        assertFalse(ParticleSpec.transition(Rgb.hex("#000000"), Rgb.hex("#FFFFFF"), 1f).toColor() == null);
    }
}
