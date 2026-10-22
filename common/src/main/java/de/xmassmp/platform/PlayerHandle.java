package de.xmassmp.platform;

import de.xmassmp.util.Vec3;

import java.util.UUID;

/**
 * Plattformneutrale Sicht auf einen Online-Spieler, z. B. für Fähigkeiten.
 *
 * <p>Ein Handle bleibt über Respawns hinweg gültig. Ist der Spieler offline, liefern die
 * Abfragen den letzten bekannten Stand und Ausgaben werden verworfen.
 */
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

    boolean isSneaking();

    /** Text in der Actionbar (Legacy-Farbcodes mit §). */
    void sendActionBar(String legacyText);

    /** Chat-Nachricht nur an diesen Spieler (Legacy-Farbcodes mit §). */
    void sendMessage(String legacyText);
}
