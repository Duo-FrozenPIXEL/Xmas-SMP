package de.xmassmp.platform;

import de.xmassmp.item.ItemSpec;
import de.xmassmp.util.Vec3;

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
}
