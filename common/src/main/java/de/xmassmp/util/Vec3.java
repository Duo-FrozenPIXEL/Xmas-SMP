package de.xmassmp.util;

/** Unveränderlicher 3D-Vektor (Weltkoordinaten oder Offsets). */
public record Vec3(double x, double y, double z) {

    public static final Vec3 ZERO = new Vec3(0, 0, 0);
    public static final Vec3 UP = new Vec3(0, 1, 0);

    public Vec3 add(Vec3 o) {
        return new Vec3(x + o.x, y + o.y, z + o.z);
    }

    public Vec3 add(double dx, double dy, double dz) {
        return new Vec3(x + dx, y + dy, z + dz);
    }

    public Vec3 subtract(Vec3 o) {
        return new Vec3(x - o.x, y - o.y, z - o.z);
    }

    public Vec3 scale(double f) {
        return new Vec3(x * f, y * f, z * f);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vec3 normalize() {
        double len = length();
        return len < 1e-9 ? ZERO : scale(1 / len);
    }

    public Vec3 cross(Vec3 o) {
        return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    public double dot(Vec3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    /** Dreht um die Y-Achse (Bogenmaß, gegen den Uhrzeigersinn von oben gesehen). */
    public Vec3 rotateY(double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(x * cos - z * sin, y, x * sin + z * cos);
    }

    public Vec3 lerp(Vec3 to, double t) {
        return new Vec3(x + (to.x - x) * t, y + (to.y - y) * t, z + (to.z - z) * t);
    }
}
