package de.xmassmp.platform;

import de.xmassmp.util.Vec3;

import java.util.UUID;

/** Plattformneutrale Sicht auf einen Online-Spieler, z. B. für Fähigkeiten. */
public interface PlayerHandle {

    UUID uuid();

    String name();

    WorldRef world();

    /** Fußposition. */
    Vec3 position();

    /** Augenposition. */
    Vec3 eyePosition();

    /** Normalisierte Blickrichtung. */
    Vec3 lookDirection();

    /** Text in der Actionbar (Legacy-Farbcodes mit §). */
    void sendActionBar(String legacyText);
}
