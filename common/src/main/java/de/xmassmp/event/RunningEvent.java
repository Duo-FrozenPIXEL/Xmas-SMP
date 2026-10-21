package de.xmassmp.event;

import java.time.Instant;

/**
 * Ein laufendes Event.
 *
 * @param end    geplantes Ende oder {@code null} = läuft bis zum manuellen Stopp
 * @param manual per Befehl gestartet
 */
public record RunningEvent(String id, Instant start, Instant end, boolean manual) {
}
