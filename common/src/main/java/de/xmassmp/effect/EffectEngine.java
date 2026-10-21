package de.xmassmp.effect;

import de.xmassmp.platform.WorldRef;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.util.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Spielt {@link EffectTimeline}s ab. Der Plattform-Adapter ruft {@link #tick()} einmal pro
 * Server-Tick auf.
 *
 * <p>Performance: Alle Effekte teilen sich ein Partikel-Budget pro Tick
 * ({@code effects.maxParticlesPerTick}); was darüber hinausgeht, wird verworfen. Die
 * {@link EffectQuality} dünnt Formen und Partikelzahlen zusätzlich aus.
 */
public final class EffectEngine {

    private final EffectSink sink;
    private final Supplier<EffectQuality> quality;
    private final IntSupplier particleBudget;
    private final RandomGenerator random;
    private final XmasLogger logger;

    private final List<Running> running = new ArrayList<>();
    private final List<Running> starting = new ArrayList<>();
    private final List<ScheduledSound> sounds = new ArrayList<>();
    private long currentTick;
    private int budgetLeft;
    private int droppedThisTick;
    private int droppedLastTick;

    public EffectEngine(EffectSink sink, Supplier<EffectQuality> quality, IntSupplier particleBudget,
                        RandomGenerator random, XmasLogger logger) {
        this.sink = sink;
        this.quality = quality;
        this.particleBudget = particleBudget;
        this.random = random;
        this.logger = logger;
        this.budgetLeft = particleBudget.getAsInt();
    }

    /** Effekt an fester Position. */
    public EffectHandle play(EffectTimeline timeline, WorldRef world, Vec3 origin) {
        return play(timeline, world, () -> origin);
    }

    /** Effekt, der einem beweglichen Anker folgt (z. B. der Spielerposition). */
    public EffectHandle play(EffectTimeline timeline, WorldRef world, Supplier<Vec3> anchor) {
        Running effect = new Running(timeline, new EffectCanvas(this, world), anchor);
        starting.add(effect);
        return effect;
    }

    public void tick() {
        currentTick++;
        droppedLastTick = droppedThisTick;
        droppedThisTick = 0;
        budgetLeft = particleBudget.getAsInt();

        playDueSounds();

        running.addAll(starting);
        starting.clear();
        Iterator<Running> it = running.iterator();
        while (it.hasNext()) {
            Running effect = it.next();
            if (effect.cancelled || effect.age >= effect.timeline.length()) {
                effect.done = true;
                it.remove();
                continue;
            }
            try {
                effect.canvas.update(effect.anchor.get(), effect.age);
                effect.timeline.runTick(effect.age, effect.canvas);
                effect.age++;
            } catch (RuntimeException e) {
                logger.error("Effekt abgebrochen (Fehler in Tick " + effect.age + ")", e);
                effect.done = true;
                it.remove();
            }
        }
    }

    public int activeCount() {
        return running.size() + starting.size();
    }

    /** Wegen des Budgets verworfene Partikel im letzten vollständigen Tick. */
    public int droppedParticlesLastTick() {
        return droppedLastTick;
    }

    public void cancelAll() {
        running.forEach(Running::cancel);
        starting.forEach(Running::cancel);
        sounds.clear();
    }

    RandomGenerator random() {
        return random;
    }

    int thinnedCount(int total) {
        if (total == 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(total * quality.get().factor()));
    }

    void emit(WorldRef world, Vec3 position, ParticleSpec spec) {
        int count = spec.count() == 1 ? 1 : Math.max(1, (int) Math.round(spec.count() * quality.get().factor()));
        if (count > budgetLeft) {
            droppedThisTick += count;
            return;
        }
        budgetLeft -= count;
        sink.particle(world, position, spec, count);
    }

    void playSounds(WorldRef world, Vec3 position, SoundStack stack) {
        for (SoundStack.Layer layer : stack.layers()) {
            if (layer.delayTicks() == 0) {
                playLayer(world, position, layer);
            } else {
                sounds.add(new ScheduledSound(currentTick + layer.delayTicks(), world, position, layer));
            }
        }
    }

    private void playDueSounds() {
        Iterator<ScheduledSound> it = sounds.iterator();
        while (it.hasNext()) {
            ScheduledSound s = it.next();
            if (s.dueTick() <= currentTick) {
                playLayer(s.world(), s.position(), s.layer());
                it.remove();
            }
        }
    }

    private void playLayer(WorldRef world, Vec3 position, SoundStack.Layer layer) {
        float variation = layer.pitchVariation() == 0 ? 0
                : (float) ((random.nextDouble() * 2 - 1) * layer.pitchVariation());
        float pitch = Math.max(0.5f, Math.min(2f, layer.pitch() + variation));
        sink.sound(world, position, layer.sound(), layer.volume(), pitch);
    }

    private record ScheduledSound(long dueTick, WorldRef world, Vec3 position, SoundStack.Layer layer) {
    }

    private static final class Running implements EffectHandle {
        final EffectTimeline timeline;
        final EffectCanvas canvas;
        final Supplier<Vec3> anchor;
        int age;
        volatile boolean cancelled;
        volatile boolean done;

        Running(EffectTimeline timeline, EffectCanvas canvas, Supplier<Vec3> anchor) {
            this.timeline = timeline;
            this.canvas = canvas;
            this.anchor = anchor;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public boolean isDone() {
            return done;
        }
    }
}
