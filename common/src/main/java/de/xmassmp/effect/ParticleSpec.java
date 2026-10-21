package de.xmassmp.effect;

import de.xmassmp.util.Rgb;
import de.xmassmp.util.Vec3;

/**
 * Ein Partikel-Typ samt Parametern.
 *
 * @param type    Partikel-ID, z. B. {@code minecraft:flame}, {@code minecraft:dust}
 * @param count   Partikel pro Punkt
 * @param spread  Streuung je Achse (Vanilla {@code delta})
 * @param speed   Geschwindigkeit
 * @param color   Farbe für {@code dust} / Startfarbe für {@code dust_color_transition}
 * @param toColor Zielfarbe für {@code dust_color_transition}
 * @param size    Größe für Dust-Partikel (0.01–4)
 */
public record ParticleSpec(String type, int count, Vec3 spread, double speed, Rgb color, Rgb toColor, float size) {

    public static final String DUST = "minecraft:dust";
    public static final String DUST_TRANSITION = "minecraft:dust_color_transition";

    public ParticleSpec {
        if (count < 1) {
            throw new IllegalArgumentException("count muss ≥ 1 sein");
        }
        if (DUST.equals(type) && color == null) {
            throw new IllegalArgumentException("dust braucht eine Farbe");
        }
        if (DUST_TRANSITION.equals(type) && (color == null || toColor == null)) {
            throw new IllegalArgumentException("dust_color_transition braucht zwei Farben");
        }
        size = Math.max(0.01f, Math.min(4f, size));
    }

    /** Partikel ohne Zusatzdaten ({@code flame}, {@code cloud}, {@code portal}, …). */
    public static ParticleSpec of(String type) {
        return new ParticleSpec(type, 1, Vec3.ZERO, 0, null, null, 1f);
    }

    public static ParticleSpec dust(Rgb color, float size) {
        return new ParticleSpec(DUST, 1, Vec3.ZERO, 0, color, null, size);
    }

    public static ParticleSpec transition(Rgb from, Rgb to, float size) {
        return new ParticleSpec(DUST_TRANSITION, 1, Vec3.ZERO, 0, from, to, size);
    }

    public ParticleSpec count(int newCount) {
        return new ParticleSpec(type, newCount, spread, speed, color, toColor, size);
    }

    public ParticleSpec spread(double dx, double dy, double dz) {
        return new ParticleSpec(type, count, new Vec3(dx, dy, dz), speed, color, toColor, size);
    }

    public ParticleSpec spread(double all) {
        return spread(all, all, all);
    }

    public ParticleSpec speed(double newSpeed) {
        return new ParticleSpec(type, count, spread, newSpeed, color, toColor, size);
    }
}
