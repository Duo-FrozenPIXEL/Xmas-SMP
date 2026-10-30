package de.xmassmp.platform;

import de.xmassmp.item.ItemSpec;
import de.xmassmp.util.Vec3;

import java.util.Map;
import java.util.UUID;

/**
 * Plattformneutrale Sicht auf einen Online-Spieler, z. B. für Fähigkeiten.
 *
 * <p>Ein Handle bleibt über Respawns hinweg gültig. Ist der Spieler offline, liefern die
 * Abfragen den letzten bekannten Stand und Ausgaben werden verworfen.
 */
public interface PlayerHandle {

    UUID uuid();

    String name();

    WorldRef world();

    /** Fußposition. */
    Vec3 position();

    /** Augenposition. */
    Vec3 eyePosition();

    /** Normalisierte Blickrichtung. */
    Vec3 lookDirection();

    boolean isSneaking();

    /** Text in der Actionbar (Legacy-Farbcodes mit §). */
    void sendActionBar(String legacyText);

    /** Chat-Nachricht nur an diesen Spieler (Legacy-Farbcodes mit §). */
    void sendMessage(String legacyText);

    /** Item ins Inventar; ist es voll, fällt es vor die Füße. */
    void giveItem(ItemSpec item);

    /**
     * Gleicht die gebundenen Kristall-Items (siehe {@link de.xmassmp.item.ItemCatalog#isOwnedCrystal})
     * im Inventar und am Mauszeiger ab. Ungebundene Kristalle (z. B. von {@code /xmas give}) bleiben
     * unberührt.
     *
     * <ul>
     *   <li>Der erste eigene Kristall wird durch {@code expected} ersetzt, wenn seine Daten abweichen
     *       (Platz im Inventar bleibt erhalten).</li>
     *   <li>{@code soulbound}: weitere eigene und alle fremden Kristalle verschwinden, auch aus
     *       geöffneten Behältern und dem Crafting-Feld; fehlt der eigene, wird er neu ins Inventar
     *       gelegt (bei vollem Inventar beim nächsten Abgleich).</li>
     *   <li>Nicht {@code soulbound}: es wird nur das Aussehen eigener Kristalle aktualisiert.</li>
     *   <li>{@code expected == null} (kein Kristall): mit {@code soulbound} werden eigene Kristalle entfernt.</li>
     * </ul>
     */
    void syncCrystalItem(ItemSpec expected, boolean soulbound);

    /** Entfernt gebundene Kristalle, die im Umkreis als Item auf dem Boden liegen. */
    void removeDroppedCrystals(double radius);

    /** Titel und Untertitel (Legacy-Farbcodes) mit Ein-/Ausblendzeiten in Ticks. */
    default void showTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
    }

    /** Item-ID in der Haupthand, z. B. {@code minecraft:glass_bottle} oder {@code xmassmp:energy_bottle}. */
    default String mainHandItem() {
        return "minecraft:air";
    }

    /** Markierungen aus {@code custom_data.xmassmp} des Items in der Haupthand; leer bei Vanilla-Items. */
    default Map<String, String> mainHandData() {
        return Map.of();
    }

    /** Ausgewählter Hotbar-Platz 0–8. */
    default int selectedSlot() {
        return 0;
    }

    /**
     * Entfernt genau ein Item aus der Haupthand, wenn es passt: gleiche Item-ID und alle Einträge aus
     * {@code requiredData} in {@code custom_data.xmassmp}. Ist {@code requiredData} leer, muss das
     * Item ein reines Vanilla-Item ohne Xmas-Markierungen sein.
     *
     * @return {@code true}, wenn ein Item entfernt wurde
     */
    default boolean consumeMainHand(String itemId, Map<String, String> requiredData) {
        return false;
    }

    /** Lässt ein Item vor die Füße fallen (jeder kann es aufheben). */
    default void dropItem(ItemSpec item) {
    }

    default boolean isAlive() {
        return true;
    }

    /** Kreativ- oder Zuschauermodus. */
    default boolean isCreative() {
        return false;
    }

    /** Schickt den Namen in der Tab-Liste neu, z. B. nach einem Tier-Wechsel. */
    default void refreshTabListName() {
    }
}
