package de.xmassmp.util;

/** RGB-Farbe als 0xRRGGBB. */
public record Rgb(int value) {

    public Rgb {
        value &= 0xFFFFFF;
    }

    public static Rgb of(int r, int g, int b) {
        return new Rgb((clamp(r) << 16) | (clamp(g) << 8) | clamp(b));
    }

    public static Rgb hex(String hex) {
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        if (s.length() != 6) {
            throw new IllegalArgumentException("Farbe muss #RRGGBB sein: " + hex);
        }
        return new Rgb(Integer.parseInt(s, 16));
    }

    public int red() {
        return (value >> 16) & 0xFF;
    }

    public int green() {
        return (value >> 8) & 0xFF;
    }

    public int blue() {
        return value & 0xFF;
    }

    /** Linearer Übergang zu {@code other}, {@code t} von 0 bis 1. */
    public Rgb lerp(Rgb other, double t) {
        double k = Math.max(0, Math.min(1, t));
        return of(
                (int) Math.round(red() + (other.red() - red()) * k),
                (int) Math.round(green() + (other.green() - green()) * k),
                (int) Math.round(blue() + (other.blue() - blue()) * k));
    }

    public String toHex() {
        return String.format("#%06X", value);
    }

    private static int clamp(int c) {
        return Math.max(0, Math.min(255, c));
    }
}
