package de.xmassmp.effect;

import de.xmassmp.platform.WorldRef;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.util.Rgb;
import de.xmassmp.util.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Display-Entities in der {@link EffectEngine}: Lebensdauer, Budget, Aufräumen. */
class DisplayEffectTest {

    private static final WorldRef WORLD = new WorldRef("overworld");

    private final List<String> calls = new ArrayList<>();
    private int alive;
    private final EffectSink sink = new EffectSink() {
        @Override
        public void particle(WorldRef world, Vec3 position, ParticleSpec spec, int count) {
        }

        @Override
        public void sound(WorldRef world, Vec3 position, String sound, float volume, float pitch) {
        }

        @Override
        public DisplayHandle spawnDisplay(WorldRef world, Vec3 position, DisplaySpec spec) {
            alive++;
            calls.add("spawn " + spec.id() + "@" + position.y());
            return new DisplayHandle() {
                private boolean removed;

                @Override
                public void moveTo(Vec3 to, int ticks) {
                    calls.add("move " + to.y() + "/" + ticks);
                }

                @Override
                public void transform(DisplayTransform transform, int ticks) {
                    calls.add("scale " + transform.scale().x() + "/" + ticks);
                }

                @Override
                public void remove() {
                    if (removed) {
                        throw new AssertionError("doppelt entfernt");
                    }
                    removed = true;
                    alive--;
                    calls.add("remove " + spec.id());
                }

                @Override
                public boolean isRemoved() {
                    return removed;
                }
            };
        }
    };

    private int limit = 10;
    private final EffectEngine engine = new EffectEngine(sink, () -> EffectQuality.HIGH, () -> 10_000,
            () -> limit, new Random(1), XmasLogger.NOOP);

    @Test
    void displaysLiveUntilEffectEnds() {
        DisplayHandle[] meteor = new DisplayHandle[1];
        engine.play(EffectTimeline.builder()
                .at(0, c -> meteor[0] = c.display(DisplaySpec.block("minecraft:magma_block").fullBright(), new Vec3(0, 10, 0)))
                .at(1, c -> meteor[0].moveTo(c.origin(), 5))
                .at(2, c -> meteor[0].transform(DisplayTransform.scaled(2), 3))
                .during(3, 2, (c, p, f) -> { })
                .build(), WORLD, new Vec3(0, 64, 0));
        for (int i = 0; i < 5; i++) {
            engine.tick();
        }
        assertEquals(1, alive);
        assertEquals(1, engine.activeDisplays());
        engine.tick();
        assertEquals(0, alive);
        assertEquals(0, engine.activeDisplays());
        assertEquals(List.of("spawn minecraft:magma_block@74.0", "move 64.0/5", "scale 2.0/3",
                "remove minecraft:magma_block"), calls);
        assertTrue(meteor[0].isRemoved());
    }

    @Test
    void earlyRemovalIsNotRepeated() {
        engine.play(EffectTimeline.builder()
                .at(0, c -> c.display(DisplaySpec.item("minecraft:vine"), Vec3.ZERO).remove())
                .during(1, 3, (c, p, f) -> { })
                .build(), WORLD, Vec3.ZERO);
        for (int i = 0; i < 6; i++) {
            engine.tick();
        }
        assertEquals(List.of("spawn minecraft:vine@0.0", "remove minecraft:vine"), calls);
    }

    @Test
    void budgetLimitsDisplays() {
        limit = 2;
        List<DisplayHandle> handles = new ArrayList<>();
        engine.play(EffectTimeline.builder()
                .at(0, c -> {
                    for (int i = 0; i < 4; i++) {
                        handles.add(c.display(DisplaySpec.block("minecraft:stone"), Vec3.ZERO));
                    }
                })
                .during(1, 5, (c, p, f) -> { })
                .build(), WORLD, Vec3.ZERO);
        engine.tick();
        assertEquals(2, alive);
        handles.get(3).moveTo(Vec3.UP, 1);
        assertEquals(2, calls.size(), "Handle über dem Budget ist wirkungslos");
    }

    @Test
    void cancelAndFailureCleanUp() {
        EffectHandle cancelled = engine.play(EffectTimeline.builder()
                .at(0, c -> c.display(DisplaySpec.block("minecraft:stone"), Vec3.ZERO))
                .during(1, 50, (c, p, f) -> { })
                .build(), WORLD, Vec3.ZERO);
        engine.play(EffectTimeline.builder()
                .at(0, c -> c.display(DisplaySpec.block("minecraft:dirt"), Vec3.ZERO))
                .at(1, c -> {
                    throw new IllegalStateException("kaputt");
                })
                .build(), WORLD, Vec3.ZERO);
        engine.tick();
        assertEquals(2, alive);
        cancelled.cancel();
        engine.tick();
        assertEquals(0, alive);
    }

    @Test
    void cancelAllRemovesImmediately() {
        engine.play(EffectTimeline.builder()
                .at(0, c -> c.display(DisplaySpec.block("minecraft:stone"), Vec3.ZERO))
                .during(1, 50, (c, p, f) -> { })
                .build(), WORLD, Vec3.ZERO);
        engine.tick();
        engine.cancelAll();
        assertEquals(0, alive);
        assertEquals(0, engine.activeDisplays());
    }

    @Test
    void sinkWithoutDisplaySupportReturnsInertHandle() {
        DisplayHandle none = new EffectSink() {
            @Override
            public void particle(WorldRef world, Vec3 position, ParticleSpec spec, int count) {
            }

            @Override
            public void sound(WorldRef world, Vec3 position, String sound, float volume, float pitch) {
            }
        }.spawnDisplay(WORLD, Vec3.ZERO, DisplaySpec.block("minecraft:stone"));
        assertFalse(none.isRemoved());
        none.remove();
        assertTrue(none.isRemoved());
    }

    @Test
    void specValidation() {
        assertThrows(IllegalArgumentException.class, () -> DisplaySpec.block(""));
        assertThrows(IllegalArgumentException.class, () -> DisplaySpec.block("minecraft:stone").brightness(16));
        DisplaySpec spec = DisplaySpec.block("minecraft:stone").scale(0.5).glow(Rgb.hex("#FF0000")).cornerAligned();
        assertEquals(0.5, spec.transform().scale().y());
        assertFalse(spec.centered());
        DisplayTransform half = DisplayTransform.IDENTITY.lerp(DisplayTransform.scaled(3).withRotation(90, 0, 0), 0.5);
        assertEquals(2.0, half.scale().x());
        assertEquals(45f, half.yaw());
    }
}
