package de.xmassmp.event;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.util.JsonFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Speichert {@link EventState} als {@code event-state.json}. */
public final class EventStateStore {

    private final Path file;
    private final XmasLogger logger;

    public EventStateStore(Path dataDirectory, XmasLogger logger) {
        this.file = dataDirectory.resolve("event-state.json");
        this.logger = logger;
    }

    public EventState load() {
        if (Files.notExists(file)) {
            return EventState.EMPTY;
        }
        try {
            JsonObject json = JsonFiles.parseLenient(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            Instant last = json.has("lastProcessed") && !json.get("lastProcessed").isJsonNull()
                    ? Instant.parse(json.get("lastProcessed").getAsString()) : null;
            List<RunningEvent> running = new ArrayList<>();
            if (json.has("running")) {
                for (JsonElement element : json.getAsJsonArray("running")) {
                    JsonObject r = element.getAsJsonObject();
                    running.add(new RunningEvent(
                            r.get("id").getAsString(),
                            Instant.parse(r.get("start").getAsString()),
                            r.has("end") && !r.get("end").isJsonNull() ? Instant.parse(r.get("end").getAsString()) : null,
                            r.has("manual") && r.get("manual").getAsBoolean()));
                }
            }
            return new EventState(last, running);
        } catch (IOException | RuntimeException e) {
            logger.error("event-state.json defekt – Event-Zustand wird zurückgesetzt", e);
            return EventState.EMPTY;
        }
    }

    public void save(EventState state) {
        JsonObject json = new JsonObject();
        json.addProperty("lastProcessed", state.lastProcessed() == null ? null : state.lastProcessed().toString());
        JsonArray running = new JsonArray();
        for (RunningEvent r : state.running()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", r.id());
            entry.addProperty("start", r.start().toString());
            entry.addProperty("end", r.end() == null ? null : r.end().toString());
            entry.addProperty("manual", r.manual());
            running.add(entry);
        }
        json.add("running", running);
        try {
            JsonFiles.writeAtomic(file, JsonFiles.GSON.toJson(json));
        } catch (IOException e) {
            logger.error("event-state.json konnte nicht gespeichert werden", e);
        }
    }
}
