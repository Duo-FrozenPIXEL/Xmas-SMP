package de.xmassmp.effect;

import de.xmassmp.util.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Partikel-Formen als Offsets relativ zum Ursprung. Alle Formen liegen horizontal bzw. stehen
 * senkrecht; {@link #orient(List, Vec3)} dreht sie in eine Blickrichtung.
 */
public final class Shapes {

    private static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));

    private Shapes() {
    }

    /** Horizontaler Kreis. {@code phase} (Bogenmaß) dreht den Startpunkt, z. B. für rotierende Ringe. */
    public static List<Vec3> ring(double radius, int points, double phase) {
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double angle = phase + 2 * Math.PI * i / points;
            out.add(new Vec3(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
        }
        return out;
    }

    /** Aufsteigende Spirale um die Y-Achse. */
    public static List<Vec3> spiral(double radius, double height, double turns, int points, double phase) {
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = points == 1 ? 0 : (double) i / (points - 1);
            double angle = phase + 2 * Math.PI * turns * t;
            out.add(new Vec3(Math.cos(angle) * radius, height * t, Math.sin(angle) * radius));
        }
        return out;
    }

    /** Gleichmäßig verteilte Kugeloberfläche (Fibonacci-Gitter). */
    public static List<Vec3> sphere(double radius, int points) {
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double y = points == 1 ? 0 : 1 - 2.0 * i / (points - 1);
            double r = Math.sqrt(Math.max(0, 1 - y * y));
            double angle = GOLDEN_ANGLE * i;
            out.add(new Vec3(Math.cos(angle) * r * radius, y * radius, Math.sin(angle) * r * radius));
        }
        return out;
    }

    /** Gerade Linie von {@code from} nach {@code to} (beide Enden enthalten). */
    public static List<Vec3> line(Vec3 from, Vec3 to, int points) {
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = points == 1 ? 0 : (double) i / (points - 1);
            out.add(from.lerp(to, t));
        }
        return out;
    }

    /**
     * Sinuswelle entlang der Linie {@code from → to}, Ausschlag horizontal quer zur Linie.
     *
     * @param waves Anzahl voller Schwingungen über die Länge
     * @param phase Verschiebung (Bogenmaß) – über die Zeit erhöht, läuft die Welle
     */
    public static List<Vec3> wave(Vec3 from, Vec3 to, double amplitude, double waves, int points, double phase) {
        Vec3 dir = to.subtract(from);
        Vec3 side = dir.cross(Vec3.UP).normalize();
        if (side.length() < 1e-9) {
            side = new Vec3(1, 0, 0);
        }
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = points == 1 ? 0 : (double) i / (points - 1);
            double offset = Math.sin(phase + 2 * Math.PI * waves * t) * amplitude;
            out.add(from.lerp(to, t).add(side.scale(offset)));
        }
        return out;
    }

    /** Dreht Offsets so, dass ihre +X-Achse in die horizontale Richtung {@code direction} zeigt. */
    public static List<Vec3> orient(List<Vec3> offsets, Vec3 direction) {
        double angle = Math.atan2(direction.z(), direction.x());
        return offsets.stream().map(v -> v.rotateY(angle)).toList();
    }
}
