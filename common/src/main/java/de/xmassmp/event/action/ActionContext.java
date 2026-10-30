package de.xmassmp.event.action;

import de.xmassmp.event.EventHost;
import de.xmassmp.text.Messages;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kontext einer Aktion.
 *
 * @param placeholders Werte für {@code {name}}-artige Platzhalter
 */
public record ActionContext(EventHost host, String eventId, Map<String, String> placeholders) {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^{}]+)}");

    public ActionContext {
        placeholders = Map.copyOf(placeholders);
    }

    /**
     * Setzt Platzhalter in einem Durchgang ein, ohne Farbcodes umzuwandeln (für Befehle).
     * Eingesetzte Werte werden nicht erneut ersetzt; unbekannte Platzhalter bleiben stehen.
     */
    public String fill(String template) {
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = placeholders.get(m.group(1));
            m.appendReplacement(out, Matcher.quoteReplacement(value != null ? value : m.group()));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** Setzt Platzhalter ein und wandelt {@code &}-Farbcodes um (für Texte). */
    public String text(String template) {
        return Messages.colorize(fill(template));
    }
}
