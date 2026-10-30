package de.xmassmp.energy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.util.JsonFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Protokolliert jede Energieänderung als JSON-Zeile in {@code energy.log}:
 * {@code {"time":"…","player":"<uuid>","name":"…","from":4,"to":5,"reason":"bottle_drink","ref":"<flaschen-id>"}}.
 * Admins sehen die letzten Einträge mit {@code /xmas energy audit}.
 */
public final class EnergyLedger {

    /** Gründe, die im Protokoll vorkommen. */
    public static final String KILL = "kill";
    public static final String DEATH = "death";
    public static final String BOTTLE_FILL = "bottle_fill";
    public static final String BOTTLE_DRINK = "bottle_drink";
    public static final String TOTEM = "totem";
    public static final String ADMIN = "admin";
    /** Kein Energiefluss, nur ein Hinweis (z. B. eine kopierte Flasche wurde entfernt). */
    public static final String SUSPICIOUS = "suspicious";

    public record Entry(String time, UUID player, String name, int from, int to, String reason, String ref) {
    }

    /** Ohne Datei, z. B. für Tests. */
    public static final EnergyLedger NONE = new EnergyLedger(null, Clock.systemUTC(), null);

    private final Path file;
    private final Clock clock;
    private final XmasLogger logger;

    public EnergyLedger(Path dataDirectory, Clock clock, XmasLogger logger) {
        this.file = dataDirectory == null ? null : dataDirectory.resolve("energy.log");
        this.clock = clock;
        this.logger = logger;
    }

    /** @param ref z. B. Flaschen-ID, Gegnername oder Admin; {@code null} wenn nichts passt */
    public synchronized void record(UUID player, String name, int from, int to, String reason, String ref) {
        if (file == null) {
            return;
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("time", clock.instant().toString());
        entry.addProperty("player", player.toString());
        entry.addProperty("name", name);
        entry.addProperty("from", from);
        entry.addProperty("to", to);
        entry.addProperty("reason", reason);
        if (ref != null) {
            entry.addProperty("ref", ref);
        }
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, JsonFiles.COMPACT.toJson(entry) + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("energy.log konnte nicht geschrieben werden", e);
        }
    }

    /** Die letzten {@code limit} Einträge, optional nur eines Spielers (älteste zuerst). */
    public synchronized List<Entry> recent(UUID player, int limit) {
        if (file == null || !Files.isRegularFile(file)) {
            return List.of();
        }
        Deque<Entry> out = new ArrayDeque<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                Entry entry = parse(line);
                if (entry != null && (player == null || player.equals(entry.player()))) {
                    out.addLast(entry);
                    if (out.size() > limit) {
                        out.removeFirst();
                    }
                }
            }
        } catch (IOException e) {
            logger.error("energy.log konnte nicht gelesen werden", e);
        }
        return List.copyOf(out);
    }

    private static Entry parse(String line) {
        try {
            JsonElement element = JsonFiles.parseLenient(line);
            if (!element.isJsonObject()) {
                return null;
            }
            JsonObject o = element.getAsJsonObject();
            return new Entry(o.get("time").getAsString(), UUID.fromString(o.get("player").getAsString()),
                    o.get("name").getAsString(), o.get("from").getAsInt(), o.get("to").getAsInt(),
                    o.get("reason").getAsString(), o.has("ref") ? o.get("ref").getAsString() : null);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
