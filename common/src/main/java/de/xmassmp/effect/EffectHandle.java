package de.xmassmp.effect;

/** Steuerung eines laufenden Effekts. */
public interface EffectHandle {

    /** Bricht den Effekt zum nächsten Tick ab. */
    void cancel();

    boolean isDone();
}
