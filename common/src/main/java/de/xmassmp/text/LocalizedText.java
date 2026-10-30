package de.xmassmp.text;

import java.util.IllegalFormatException;
import java.util.List;

/**
 * Übersetzbarer Text: Schlüssel aus {@code assets/xmassmp/lang/*.json} plus deutscher Fallback,
 * falls der Client das Ressourcenpaket nicht hat.
 *
 * @param key      Übersetzungsschlüssel
 * @param fallback Fallback mit {@code %s}-Platzhaltern
 * @param args     Argumente für die Platzhalter (selbst wieder übersetzbar)
 * @param color    Legacy-Farbcode ({@code 0-9a-f}) oder {@code 0} für Standard
 */
public record LocalizedText(String key, String fallback, List<LocalizedText> args, char color) {

    public LocalizedText {
        args = List.copyOf(args);
    }

    public static LocalizedText of(String key, String fallback) {
        return new LocalizedText(key, fallback, List.of(), (char) 0);
    }

    /** Wörtlicher Text ohne Übersetzung. */
    public static LocalizedText literal(String text) {
        return new LocalizedText(null, text, List.of(), (char) 0);
    }

    public LocalizedText withArgs(LocalizedText... arguments) {
        return new LocalizedText(key, fallback, List.of(arguments), color);
    }

    public LocalizedText withColor(char legacyColor) {
        return new LocalizedText(key, fallback, args, legacyColor);
    }

    public boolean isLiteral() {
        return key == null;
    }

    /** Fallback mit eingesetzten Argumenten, z. B. für Logs und Tests. */
    public String toPlainString() {
        List<String> values = args.stream().map(LocalizedText::toPlainString).toList();
        if (values.isEmpty()) {
            return fallback;
        }
        try {
            return String.format(fallback, values.toArray());
        } catch (IllegalFormatException e) {
            // Ungültiges Format im Fallback (z. B. "50% schneller"): Argumente ohne Formatierung anhängen
            return fallback + " " + String.join(" ", values);
        }
    }
}
