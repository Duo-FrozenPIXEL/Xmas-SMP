package de.xmassmp.admin;

import com.google.gson.JsonObject;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.util.JsonFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.util.function.BooleanSupplier;

/**
 * Protokolliert Admin-Änderungen als JSON-Zeilen in {@code audit.log}:
 * {@code {"time":"…","actor":"…","action":"…","target":"…","details":"…"}}.
 */
public final class AuditLog {

    private final Path file;
    private final BooleanSupplier enabled;
    private final Clock clock;
    private final XmasLogger logger;

    public AuditLog(Path dataDirectory, BooleanSupplier enabled, Clock clock, XmasLogger logger) {
        this.file = dataDirectory.resolve("audit.log");
        this.enabled = enabled;
        this.clock = clock;
        this.logger = logger;
    }

    public Path file() {
        return file;
    }

    /**
     * @param actor  Spielername oder {@code Konsole}
     * @param target betroffener Spieler/Event oder {@code null}
     */
    public synchronized void record(String actor, String action, String target, String details) {
        if (!enabled.getAsBoolean()) {
            return;
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("time", clock.instant().toString());
        entry.addProperty("actor", actor);
        entry.addProperty("action", action);
        if (target != null) {
            entry.addProperty("target", target);
        }
        if (details != null) {
            entry.addProperty("details", details);
        }
        String line = JsonFiles.COMPACT.toJson(entry) + System.lineSeparator();
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("audit.log konnte nicht geschrieben werden", e);
        }
    }
}
