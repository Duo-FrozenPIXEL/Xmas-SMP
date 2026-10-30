package de.xmassmp.config;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ändert einzelne Werte in einer JSONC-Datei, ohne Kommentare und Formatierung anzutasten.
 * So bleiben die Erklärungen in {@code config.jsonc} erhalten, wenn die Admin-GUI speichert.
 *
 * <p>Pfadsegmente sind Objekt-Schlüssel oder {@code id=<wert>} für das Array-Element, dessen
 * {@code "id"} diesen Wert hat (z. B. {@code ["events", "id=eve", "enabled"]}).
 */
public final class JsoncEditor {

    private final String text;
    private int pos;

    private JsoncEditor(String text) {
        this.text = text;
    }

    /**
     * Ersetzt den Wert unter {@code path} durch {@code jsonLiteral} (z. B. {@code true},
     * {@code 1.5} oder {@code "text"}). Fehlende Objekt-Schlüssel werden angelegt.
     *
     * @throws IllegalArgumentException wenn der Text kein lesbares JSONC ist oder ein
     *                                  Array-Element fehlt
     */
    public static String set(String text, List<String> path, String jsonLiteral) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Pfad ist leer");
        }
        JsoncEditor editor = new JsoncEditor(text);
        editor.skipBlank();
        return editor.setIn(editor.pos, path, 0, jsonLiteral);
    }

    /** JSON-Darstellung eines Werts: Zahlen, Wahrheitswerte, Texte und Enums. */
    public static String literal(Object value) {
        if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            return value.toString();
        }
        if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("Ungültige Zahl: " + d);
            }
            return d == Math.rint(d) ? String.valueOf((long) d) + ".0"
                    : BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
        }
        if (value instanceof Enum<?> e) {
            return quote(e.name());
        }
        if (value instanceof String s) {
            return quote(s);
        }
        throw new IllegalArgumentException("Nicht unterstützter Wert: " + value);
    }

    // ------------------------------------------------------------------ Suche

    private String setIn(int valueStart, List<String> path, int index, String literal) {
        pos = valueStart;
        if (index == path.size()) {
            skipValue();
            return text.substring(0, valueStart) + literal + text.substring(pos);
        }
        String segment = path.get(index);
        char c = peek();
        if (c == '{' && !segment.startsWith("id=")) {
            return setInObject(path, index, literal);
        }
        if (c == '[' && segment.startsWith("id=")) {
            return setInArray(path, index, literal, segment.substring(3));
        }
        throw error("\"" + segment + "\" erwartet " + (segment.startsWith("id=") ? "eine Liste" : "ein Objekt"));
    }

    private String setInObject(List<String> path, int index, String literal) {
        int brace = pos;
        pos++;
        int firstMember = -1;
        while (true) {
            skipBlank();
            char c = peek();
            if (c == '}') {
                break;
            }
            if (c == ',') {
                pos++;
                continue;
            }
            if (firstMember < 0) {
                firstMember = pos;
            }
            String key = readKey();
            skipBlank();
            expect(':');
            skipBlank();
            if (key.equals(path.get(index))) {
                return setIn(pos, path, index + 1, literal);
            }
            skipValue();
        }
        // Schlüssel fehlt: als ersten Eintrag des Objekts einfügen.
        String member = quote(path.get(index)) + ": " + nested(path, index + 1, literal);
        if (firstMember < 0) {
            return text.substring(0, brace + 1) + " " + member + " " + text.substring(brace + 1);
        }
        String separator = sameLine(brace, firstMember) ? ", " : ",\n" + indentOf(firstMember);
        return text.substring(0, firstMember) + member + separator + text.substring(firstMember);
    }

    private String setInArray(List<String> path, int index, String literal, String id) {
        pos++;
        while (true) {
            skipBlank();
            char c = peek();
            if (c == ']') {
                throw error("kein Eintrag mit \"id\": \"" + id + "\"");
            }
            if (c == ',') {
                pos++;
                continue;
            }
            int elementStart = pos;
            if (c == '{' && id.equals(idOf(elementStart))) {
                return setIn(elementStart, path, index + 1, literal);
            }
            pos = elementStart;
            skipValue();
        }
    }

    /** Wert von {@code "id"} im Objekt ab {@code start} oder {@code null}. */
    private String idOf(int start) {
        pos = start + 1;
        String found = null;
        while (true) {
            skipBlank();
            char c = peek();
            if (c == '}') {
                pos++;
                return found;
            }
            if (c == ',') {
                pos++;
                continue;
            }
            String key = readKey();
            skipBlank();
            expect(':');
            skipBlank();
            if (key.equals("id") && peek() == '"') {
                found = readString();
            } else {
                skipValue();
            }
        }
    }

    private static String nested(List<String> path, int from, String literal) {
        if (from == path.size()) {
            return literal;
        }
        if (path.get(from).startsWith("id=")) {
            throw new IllegalArgumentException("Liste \"" + path.get(from - 1) + "\" fehlt");
        }
        return "{ " + quote(path.get(from)) + ": " + nested(path, from + 1, literal) + " }";
    }

    // ------------------------------------------------------------------ Lesen

    private char peek() {
        if (pos >= text.length()) {
            throw error("unerwartetes Dateiende");
        }
        return text.charAt(pos);
    }

    private void expect(char c) {
        if (peek() != c) {
            throw error("'" + c + "' erwartet");
        }
        pos++;
    }

    /** Überspringt Leerraum und Kommentare ({@code //}, {@code #}, {@code /* *}{@code /}). */
    private void skipBlank() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (Character.isWhitespace(c)) {
                pos++;
            } else if (!skipComment()) {
                return;
            }
        }
    }

    private boolean skipComment() {
        if (pos >= text.length()) {
            return false;
        }
        char c = text.charAt(pos);
        if (c == '#' || text.startsWith("//", pos)) {
            int end = text.indexOf('\n', pos);
            pos = end < 0 ? text.length() : end + 1;
            return true;
        }
        if (text.startsWith("/*", pos)) {
            int end = text.indexOf("*/", pos + 2);
            if (end < 0) {
                throw error("Kommentar ohne Ende");
            }
            pos = end + 2;
            return true;
        }
        return false;
    }

    private String readKey() {
        if (peek() == '"') {
            return readString();
        }
        int start = pos;
        while (pos < text.length() && isBareKeyChar(text.charAt(pos))) {
            pos++;
        }
        if (start == pos) {
            throw error("Schlüssel erwartet");
        }
        return text.substring(start, pos);
    }

    private static boolean isBareKeyChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '$';
    }

    private String readString() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            char c = peek();
            pos++;
            if (c == '"') {
                return out.toString();
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            char escaped = peek();
            pos++;
            switch (escaped) {
                case 'n' -> out.append('\n');
                case 't' -> out.append('\t');
                case 'r' -> out.append('\r');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'u' -> {
                    if (pos + 4 > text.length()) {
                        throw error("unvollständiges \\u");
                    }
                    out.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                    pos += 4;
                }
                default -> out.append(escaped);
            }
        }
    }

    private void skipValue() {
        char c = peek();
        if (c == '"') {
            readString();
            return;
        }
        if (c == '{' || c == '[') {
            int depth = 0;
            do {
                char ch = peek();
                if (ch == '"') {
                    readString();
                    continue;
                }
                if ((ch == '/' || ch == '#') && skipComment()) {
                    continue;
                }
                if (ch == '{' || ch == '[') {
                    depth++;
                } else if (ch == '}' || ch == ']') {
                    depth--;
                }
                pos++;
            } while (depth > 0);
            return;
        }
        int start = pos;
        while (pos < text.length()) {
            char ch = text.charAt(pos);
            if (ch == ',' || ch == '}' || ch == ']' || Character.isWhitespace(ch) || ch == '#'
                    || text.startsWith("//", pos) || text.startsWith("/*", pos)) {
                break;
            }
            pos++;
        }
        if (start == pos) {
            throw error("Wert erwartet");
        }
    }

    // ------------------------------------------------------------------ Hilfen

    private boolean sameLine(int a, int b) {
        return text.substring(a, b).indexOf('\n') < 0;
    }

    private String indentOf(int position) {
        int lineStart = text.lastIndexOf('\n', position - 1) + 1;
        int end = lineStart;
        while (end < position && (text.charAt(end) == ' ' || text.charAt(end) == '\t')) {
            end++;
        }
        return text.substring(lineStart, end);
    }

    static String quote(String s) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\t' -> out.append("\\t");
                case '\r' -> out.append("\\r");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private IllegalArgumentException error(String message) {
        int line = 1;
        for (int i = 0; i < Math.min(pos, text.length()); i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return new IllegalArgumentException(message + " (Zeile " + line + ")");
    }
}
