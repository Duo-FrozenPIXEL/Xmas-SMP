package de.xmassmp.text;

/** Chat-Präfix und Hilfen für Legacy-Farbcodes (§). */
public final class Messages {

    public static final String PREFIX = "§8[§cXmas§8] §7";

    private Messages() {
    }

    public static String prefixed(String message) {
        return PREFIX + message;
    }

    /** Wandelt {@code &}-Farbcodes aus Konfigurationsdateien in §-Codes um ({@code &&} = {@code &}). */
    public static String colorize(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (next == '&') {
                    out.append('&');
                    i++;
                    continue;
                }
                if ("0123456789abcdefklmnorABCDEFKLMNOR".indexOf(next) >= 0) {
                    out.append('§').append(Character.toLowerCase(next));
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }
}
