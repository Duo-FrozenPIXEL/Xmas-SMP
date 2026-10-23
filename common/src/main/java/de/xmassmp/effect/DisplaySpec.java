package de.xmassmp.effect;

import de.xmassmp.util.Rgb;

/**
 * Ein Block- oder Item-Display für 3D-Effekte (Felsbrocken, Meteore, Ranken).
 *
 * <p>Displays sind reine Optik: Sie haben keine Hitbox, werden nicht gespeichert und
 * verschwinden spätestens mit dem Effekt, der sie erzeugt hat.
 *
 * @param kind       Block oder Item
 * @param id         Block- bzw. Item-ID, z. B. {@code minecraft:magma_block}
 * @param transform  Anfangslage
 * @param centered   nur Blöcke: um die Position zentriert statt an der Blockecke
 * @param glowColor  Leuchtumriss in dieser Farbe oder {@code null}
 * @param brightness feste Helligkeit 0–15 (z. B. 15 für Glut) oder {@code -1} für Umgebungslicht
 */
public record DisplaySpec(Kind kind, String id, DisplayTransform transform, boolean centered, Rgb glowColor,
                          int brightness) {

    public enum Kind { BLOCK, ITEM }

    public DisplaySpec {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Display braucht eine Block- oder Item-ID");
        }
        if (brightness < -1 || brightness > 15) {
            throw new IllegalArgumentException("brightness muss -1 oder 0–15 sein: " + brightness);
        }
        if (transform == null) {
            transform = DisplayTransform.IDENTITY;
        }
    }

    public static DisplaySpec block(String blockId) {
        return new DisplaySpec(Kind.BLOCK, blockId, DisplayTransform.IDENTITY, true, null, -1);
    }

    public static DisplaySpec item(String itemId) {
        return new DisplaySpec(Kind.ITEM, itemId, DisplayTransform.IDENTITY, true, null, -1);
    }

    public DisplaySpec transform(DisplayTransform newTransform) {
        return new DisplaySpec(kind, id, newTransform, centered, glowColor, brightness);
    }

    public DisplaySpec scale(double scale) {
        return transform(transform.withScale(scale));
    }

    /** Block an der Ecke statt mittig ausrichten (wie ein platzierter Block). */
    public DisplaySpec cornerAligned() {
        return new DisplaySpec(kind, id, transform, false, glowColor, brightness);
    }

    public DisplaySpec glow(Rgb color) {
        return new DisplaySpec(kind, id, transform, centered, color, brightness);
    }

    public DisplaySpec brightness(int level) {
        return new DisplaySpec(kind, id, transform, centered, glowColor, level);
    }

    public DisplaySpec fullBright() {
        return brightness(15);
    }
}
