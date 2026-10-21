package de.xmassmp.effect;

import de.xmassmp.platform.WorldRef;
import de.xmassmp.util.Vec3;

/** Plattform-Schnittstelle zum tatsächlichen Senden von Partikeln und Sounds. */
public interface EffectSink {

    /** Spawnt {@code count} Partikel bei {@code position}; {@code spec.count()} ist bereits eingerechnet. */
    void particle(WorldRef world, Vec3 position, ParticleSpec spec, int count);

    void sound(WorldRef world, Vec3 position, String sound, float volume, float pitch);
}
