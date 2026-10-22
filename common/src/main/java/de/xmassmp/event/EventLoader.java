package de.xmassmp.event;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import de.xmassmp.event.action.EventAction;
import de.xmassmp.event.action.EventActionRegistry;
import de.xmassmp.util.JsonFiles;
import de.xmassmp.util.ResourceIds;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static de.xmassmp.event.action.EventActionRegistry.optString;

/**
 * Liest {@code events.jsonc}. Fehlerhafte Events werden übersprungen und gemeldet, der Rest wird
 * trotzdem geladen.
 */
public final class EventLoader {

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]+");

    private final EventActionRegistry actions;

    public EventLoader(EventActionRegistry actions) {
        this.actions = actions;
    }

    public record Result(EventMessages messages, List<EventDefinition> events, List<String> errors) {
        public Result {
            events = List.copyOf(events);
            errors = List.copyOf(errors);
        }
    }

    public Result parse(String text) {
        List<String> errors = new ArrayList<>();
        JsonObject root;
        try {
            JsonElement parsed = JsonFiles.parseLenient(text);
            if (parsed == null || !parsed.isJsonObject()) {
                throw new JsonParseException("Wurzel muss ein Objekt sein");
            }
            root = parsed.getAsJsonObject();
        } catch (JsonParseException e) {
            errors.add("events.jsonc ist kein gültiges JSON: " + e.getMessage());
            return new Result(EventMessages.DEFAULTS, List.of(), errors);
        }

        EventMessages messages = parseMessages(root.has("messages") && root.get("messages").isJsonObject()
                ? root.getAsJsonObject("messages") : null, errors);
        List<EventDefinition> events = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        JsonArray array = root.has("events") && root.get("events").isJsonArray()
                ? root.getAsJsonArray("events") : new JsonArray();
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            String label = "events[" + i + "]";
            try {
                if (!element.isJsonObject()) {
                    throw new EventConfigException("muss ein Objekt sein");
                }
                label = optString(element.getAsJsonObject(), "id", label);
                EventDefinition def = parseEvent(element.getAsJsonObject());
                if (!ids.add(def.id())) {
                    throw new EventConfigException("ID doppelt vergeben");
                }
                events.add(def);
            } catch (EventConfigException | RuntimeException e) {
                errors.add("Event " + label + ": " + e.getMessage());
            }
        }
        return new Result(messages, events, errors);
    }

    private static EventMessages parseMessages(JsonObject json, List<String> errors) {
        EventMessages d = EventMessages.DEFAULTS;
        if (json == null) {
            return d;
        }
        return new EventMessages(
                optString(json, "announce", d.announce()),
                optSound(json, "announceSound", d.announceSound(), errors),
                optString(json, "countdownTitle", d.countdownTitle()),
                optString(json, "countdownSubtitle", d.countdownSubtitle()),
                optSound(json, "countdownSound", d.countdownSound(), errors));
    }

    /** Leer = kein Sound. Ungültige IDs fallen auf den Standard zurück, statt zur Laufzeit zu werfen. */
    private static String optSound(JsonObject json, String key, String fallback, List<String> errors) {
        String sound = optString(json, key, fallback);
        if (sound.isEmpty() || ResourceIds.isValid(sound)) {
            return sound;
        }
        errors.add("messages." + key + ": ungültige Sound-ID '" + sound + "' (nur a-z, 0-9, _ . - /) – "
                + fallback + " verwendet");
        return fallback;
    }

    private EventDefinition parseEvent(JsonObject json) throws EventConfigException {
        String id = EventActionRegistry.requireString(json, "id");
        if (!ID.matcher(id).matches()) {
            throw new EventConfigException("ID '" + id + "' darf nur a-z, 0-9, _ und - enthalten");
        }
        String name = optString(json, "name", id);
        boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();

        List<DateRule> dates = new ArrayList<>();
        if (json.has("dates")) {
            for (JsonElement entry : json.getAsJsonArray("dates")) {
                if (entry.isJsonObject()) {
                    JsonObject range = entry.getAsJsonObject();
                    String from = EventActionRegistry.requireString(range, "from");
                    dates.add(DateRule.parse(from, optString(range, "to", from)));
                } else {
                    dates.add(DateRule.parse(entry.getAsString(), entry.getAsString()));
                }
            }
        }

        Set<DayOfWeek> weekdays = new HashSet<>();
        if (json.has("weekdays")) {
            for (JsonElement entry : json.getAsJsonArray("weekdays")) {
                try {
                    weekdays.add(DayOfWeek.valueOf(entry.getAsString().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    throw new EventConfigException("Unbekannter Wochentag '" + entry.getAsString()
                            + "' (MONDAY … SUNDAY)");
                }
            }
        }

        List<LocalTime> times = new ArrayList<>();
        if (!json.has("times") || json.getAsJsonArray("times").isEmpty()) {
            throw new EventConfigException("'times' fehlt (z. B. [\"18:00\"])");
        }
        for (JsonElement entry : json.getAsJsonArray("times")) {
            try {
                times.add(LocalTime.parse(entry.getAsString()));
            } catch (DateTimeParseException e) {
                throw new EventConfigException("Ungültige Uhrzeit '" + entry.getAsString() + "' (HH:mm)");
            }
        }

        double minutes = EventActionRegistry.optDouble(json, "durationMinutes", 0);
        if (minutes < 0) {
            throw new EventConfigException("'durationMinutes' darf nicht negativ sein");
        }
        Duration duration = Duration.ofSeconds(Math.round(minutes * 60));

        List<Integer> announce = new ArrayList<>();
        if (json.has("announceMinutes")) {
            for (JsonElement entry : json.getAsJsonArray("announceMinutes")) {
                announce.add(entry.getAsInt());
            }
        }
        int countdown = EventActionRegistry.optInt(json, "countdownSeconds", 0);
        if (countdown < 0 || countdown > 60) {
            throw new EventConfigException("'countdownSeconds' muss 0–60 sein");
        }

        Map<String, Double> modifiers = new LinkedHashMap<>();
        if (json.has("modifiers")) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("modifiers").entrySet()) {
                double value = e.getValue().getAsDouble();
                if (value < 0) {
                    throw new EventConfigException("Modifikator '" + e.getKey() + "' darf nicht negativ sein");
                }
                modifiers.put(e.getKey(), value);
            }
        }

        return new EventDefinition(id, name, enabled,
                new EventSchedule(dates, weekdays, times), duration, announce, countdown,
                parseActions(json, "onStart"), parseActions(json, "onEnd"), modifiers);
    }

    private List<EventAction> parseActions(JsonObject json, String key) throws EventConfigException {
        List<EventAction> out = new ArrayList<>();
        if (json.has(key)) {
            JsonArray array = json.getAsJsonArray(key);
            for (int i = 0; i < array.size(); i++) {
                try {
                    out.add(actions.parse(array.get(i)));
                } catch (EventConfigException e) {
                    throw new EventConfigException(key + "[" + i + "]: " + e.getMessage());
                }
            }
        }
        return out;
    }
}
