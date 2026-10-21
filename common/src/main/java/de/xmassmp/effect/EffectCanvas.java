package de.xmassmp.effect;

import de.xmassmp.platform.WorldRef;
import de.xmassmp.util.Vec3;

import java.util.List;
import java.util.random.RandomGenerator;

/** Zeichenfläche eines laufenden Effekts. Offsets sind relativ zum (ggf. mitwandernden) Ursprung. */
public final class EffectCanvas {

    private final EffectEngine engine;
    private final WorldRef world;
    private Vec3 origin;
    private int age;

    EffectCanvas(EffectEngine engine, WorldRef world) {
        this.engine = engine;
        this.world = world;
    }

    void update(Vec3 newOrigin, int newAge) {
        this.origin = newOrigin;
        this.age = newAge;
    }

    public WorldRef world() {
        return world;
    }

    /** Aktueller Ursprung in Weltkoordinaten. */
    public Vec3 origin() {
        return origin;
    }

    /** Ticks seit Effektstart. */
    public int age() {
        return age;
    }

    public RandomGenerator random() {
        return engine.random();
    }

    public void particle(ParticleSpec spec, Vec3 offset) {
        particleAt(spec, origin.add(offset));
    }

    /** Partikel an einer absoluten Weltposition. */
    public void particleAt(ParticleSpec spec, Vec3 position) {
        engine.emit(world, position, spec);
    }

    /** Partikel an allen Offsets einer Form; bei geringerer Qualität wird die Form ausgedünnt. */
    public void particles(ParticleSpec spec, List<Vec3> offsets) {
        int total = offsets.size();
        int keep = engine.thinnedCount(total);
        for (int i = 0; i < keep; i++) {
            particle(spec, offsets.get((int) ((long) i * total / keep)));
        }
    }

    public void sound(SoundStack sounds) {
        engine.playSounds(world, origin, sounds);
    }
}
