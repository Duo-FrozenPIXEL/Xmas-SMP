package de.xmassmp.event;

import java.time.Instant;
import java.util.List;

/**
 * Persistenter Zustand des Schedulers.
 *
 * @param lastProcessed bis zu diesem Zeitpunkt sind alle Auslöser abgearbeitet ({@code null} = noch nie)
 */
public record EventState(Instant lastProcessed, List<RunningEvent> running) {

    public static final EventState EMPTY = new EventState(null, List.of());

    public EventState {
        running = List.copyOf(running);
    }
}
