package de.xmassmp.platform;

import de.xmassmp.effect.EffectSink;
import de.xmassmp.event.EventHost;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Alles, was der Kern von einer Server-Plattform (Fabric, Paper, NeoForge) braucht.
 *
 * <p>Zeitsteuerung läuft über {@link de.xmassmp.XmasCore#tick()}, das der Adapter einmal pro
 * Server-Tick aufruft. Ein eigener Scheduler ist dadurch nicht nötig.
 */
public interface Platform {

    /** Ordner für {@code config.jsonc} und {@code events.jsonc}. */
    Path configDirectory();

    /** Ordner für Laufzeitdaten (Spielerdaten, Event-Zustand, Audit-Log), meist pro Welt. */
    Path dataDirectory();

    XmasLogger logger();

    EffectSink effectSink();

    EventHost eventHost();

    /** Alle Spieler, die gerade online sind. */
    List<PlayerHandle> onlinePlayers();

    Optional<PlayerHandle> player(UUID uuid);
}
