package de.xmassmp.event.action;

import de.xmassmp.event.EventHost;
import de.xmassmp.text.Messages;

import java.util.Map;

/**
 * Kontext einer Aktion.
 *
 * @param placeholders Werte für {@code {name}}-artige Platzhalter
 */
public record ActionContext(EventHost host, String eventId, Map<String, String> placeholders) {

    public ActionContext {
        placeholders = Map.copyOf(placeholders);
    }

    /** Setzt Platzhalter ein, ohne Farbcodes umzuwandeln (für Befehle). */
    public String fill(String template) {
        String out = template;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }

    /** Setzt Platzhalter ein und wandelt {@code &}-Farbcodes um (für Texte). */
    public String text(String template) {
        return Messages.colorize(fill(template));
    }
}
