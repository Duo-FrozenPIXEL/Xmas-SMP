package de.xmassmp.bottle;

import com.google.gson.reflect.TypeToken;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.util.JsonFiles;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Flaschen-Register: Der Server ist die einzige Wahrheit darüber, welche Energy Bottle noch Energie
 * enthält. Das Item trägt nur die ID ({@code custom_data.xmassmp.bottle}).
 *
 * <p>Jede Flasche wird genau einmal geschlossen (getrunken oder zerlegt). Kopien – durch Dupe-Glitches
 * oder Creative-Mittelklick – haben dieselbe ID und gehen danach ins Leere; gefälschte Flaschen haben
 * gar keinen Eintrag. Alle Aufrufe laufen auf dem Server-Thread, deshalb sind Prüfen und Schließen
 * ein Schritt. Jede Änderung wird sofort gespeichert ({@code bottles.json}), damit ein Absturz keine
 * „Geister-Flaschen“ hinterlässt.
 */
public final class BottleRegistry {

    public enum State { ACTIVE, USED, UNKNOWN }

    /** Ein Eintrag; {@code closed*} sind leer, solange die Flasche aktiv ist. */
    public static final class Entry {
        public String origin;
        public String creator;
        public String created;
        public String closed;
        public String closedBy;
        public String closedReason;

        boolean isActive() {
            return closed == null;
        }
    }

    private static final java.lang.reflect.Type TYPE = new TypeToken<LinkedHashMap<String, Entry>>() {
    }.getType();

    private final Path file;
    private final Clock clock;
    private final XmasLogger logger;
    private Map<String, Entry> entries = new LinkedHashMap<>();

    /** @param dataDirectory {@code null} = nur im Speicher (Tests) */
    public BottleRegistry(Path dataDirectory, Clock clock, XmasLogger logger) {
        this.file = dataDirectory == null ? null : dataDirectory.resolve("bottles.json");
        this.clock = clock;
        this.logger = logger;
    }

    /** Lädt {@code bottles.json}. Eine defekte Datei wird gesichert; alle alten Flaschen sind dann wirkungslos. */
    public void load() {
        entries = new LinkedHashMap<>();
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, Entry> loaded = JsonFiles.readLenient(reader, TYPE);
            if (loaded != null) {
                loaded.values().removeIf(java.util.Objects::isNull);
                entries = new LinkedHashMap<>(loaded);
            }
        } catch (IOException | RuntimeException e) {
            Path backup = file.resolveSibling("bottles.json.broken-" + clock.millis());
            try {
                Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveError) {
                e.addSuppressed(moveError);
            }
            logger.error("bottles.json ist defekt und wurde nach " + backup.getFileName()
                    + " gesichert. Bisherige Energy Bottles sind wirkungslos.", e);
        }
    }

    /**
     * Legt eine neue, aktive Flasche an.
     *
     * @param origin  Herkunft, z. B. {@code fill}, {@code overflow}, {@code admin}
     * @param creator Spielername oder Admin
     */
    public UUID create(String origin, String creator) {
        UUID id = UUID.randomUUID();
        Entry entry = new Entry();
        entry.origin = origin;
        entry.creator = creator;
        entry.created = clock.instant().toString();
        entries.put(id.toString(), entry);
        save();
        return id;
    }

    public State state(String id) {
        Entry entry = id == null ? null : entries.get(id);
        if (entry == null) {
            return State.UNKNOWN;
        }
        return entry.isActive() ? State.ACTIVE : State.USED;
    }

    /**
     * Schließt eine aktive Flasche. Nur wenn das klappt, darf ihr Inhalt (Energie oder Fragments)
     * ausgegeben werden.
     *
     * @return {@code false}, wenn die Flasche unbekannt oder schon verbraucht ist
     */
    public boolean close(String id, String by, String reason) {
        Entry entry = id == null ? null : entries.get(id);
        if (entry == null || !entry.isActive()) {
            return false;
        }
        entry.closed = clock.instant().toString();
        entry.closedBy = by;
        entry.closedReason = reason;
        save();
        return true;
    }

    /** IDs aller noch aktiven Flaschen (älteste zuerst). */
    public List<String> activeIds() {
        return entries.entrySet().stream().filter(e -> e.getValue().isActive()).map(Map.Entry::getKey).toList();
    }

    public Entry entry(String id) {
        return entries.get(id);
    }

    private void save() {
        if (file == null) {
            return;
        }
        try {
            JsonFiles.writeAtomic(file, JsonFiles.GSON.toJson(entries, TYPE));
        } catch (IOException e) {
            logger.error("bottles.json konnte nicht gespeichert werden", e);
        }
    }
}
