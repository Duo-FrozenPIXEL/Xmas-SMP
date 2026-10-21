package de.xmassmp.effect;

/** Konfigurierbare Partikel-Qualität. Der Faktor dünnt Formen aus und senkt Partikelzahlen. */
public enum EffectQuality {
    LOW(0.35),
    MEDIUM(0.65),
    HIGH(1.0);

    private final double factor;

    EffectQuality(double factor) {
        this.factor = factor;
    }

    public double factor() {
        return factor;
    }
}
