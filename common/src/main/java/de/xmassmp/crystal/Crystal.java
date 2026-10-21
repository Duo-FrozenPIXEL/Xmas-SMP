package de.xmassmp.crystal;

import de.xmassmp.text.LocalizedText;
import de.xmassmp.util.Rgb;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Ein magischer Kristall.
 *
 * @param id          stabile ID (Konfiguration, Spielerdaten, Befehle), z. B. {@code fire}
 * @param nameFallback deutscher Anzeigename, falls der Client das Pack nicht hat
 * @param gemTexture  Texturname im Pack ({@code strength}, {@code fire}, …), siehe docs/resourcepack.md
 * @param gemIndex    Gem-Index für Custom-Model-Data
 * @param color       Hauptfarbe (Partikel, Lore)
 * @param accentColor Zweitfarbe für Farbverläufe ({@code dust_color_transition})
 * @param chatColor   Legacy-Farbcode für Chat/Lore
 * @param hudIcons    Font-Zeichen der zwei Fähigkeits-Icons
 */
public record Crystal(
        String id,
        String nameFallback,
        String gemTexture,
        int gemIndex,
        Rgb color,
        Rgb accentColor,
        char chatColor,
        String hudIcons) {

    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");

    public Crystal {
        if (!ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Ungültige Kristall-ID: " + id);
        }
        if (hudIcons.codePointCount(0, hudIcons.length()) != 2) {
            throw new IllegalArgumentException("Kristall " + id + " braucht genau zwei HUD-Icons");
        }
    }

    public LocalizedText displayName() {
        return LocalizedText.of("crystal.xmassmp." + id, nameFallback).withColor(chatColor);
    }

    /** Icon der Fähigkeit 1 oder 2. */
    public String hudIcon(int slot) {
        int first = hudIcons.codePointAt(0);
        int second = hudIcons.codePointAt(Character.charCount(first));
        return Character.toString(slot == 1 ? first : second);
    }

    @Override
    public String toString() {
        return "Crystal[" + id.toUpperCase(Locale.ROOT) + "]";
    }
}
