package de.xmassmp.event;

/** Fehler in {@code events.jsonc}. */
public final class EventConfigException extends Exception {

    public EventConfigException(String message) {
        super(message);
    }
}
