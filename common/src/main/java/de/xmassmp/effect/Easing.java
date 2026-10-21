package de.xmassmp.effect;

import java.util.function.DoubleUnaryOperator;

/** Easing-Kurven für Animationsphasen (Eingabe und Ausgabe 0–1). */
public enum Easing {
    LINEAR(t -> t),
    EASE_IN(t -> t * t * t),
    EASE_OUT(t -> 1 - Math.pow(1 - t, 3)),
    EASE_IN_OUT(t -> t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2),
    /** Leichtes Überschwingen am Ende, z. B. für Schockwellen. */
    BACK_OUT(t -> {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    });

    private final DoubleUnaryOperator curve;

    Easing(DoubleUnaryOperator curve) {
        this.curve = curve;
    }

    public double apply(double t) {
        return curve.applyAsDouble(Math.max(0, Math.min(1, t)));
    }
}
