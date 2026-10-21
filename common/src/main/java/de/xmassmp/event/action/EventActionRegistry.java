package de.xmassmp.event.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.xmassmp.event.EventConfigException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Bekannte Aktionstypen. Standard: broadcast, title, sound, command. */
public final class EventActionRegistry {

    private final Map<String, EventActionType> types = new LinkedHashMap<>();

    public static EventActionRegistry withDefaults() {
        EventActionRegistry registry = new EventActionRegistry();
        registry.register(simple("broadcast", json -> {
            String message = requireString(json, "message");
            return ctx -> ctx.host().broadcast(ctx.text(message));
        }));
        registry.register(simple("title", json -> {
            String title = optString(json, "title", "");
            String subtitle = optString(json, "subtitle", "");
            int fadeIn = optInt(json, "fadeIn", 10);
            int stay = optInt(json, "stay", 70);
            int fadeOut = optInt(json, "fadeOut", 20);
            if (title.isEmpty() && subtitle.isEmpty()) {
                throw new EventConfigException("title: 'title' oder 'subtitle' angeben");
            }
            return ctx -> ctx.host().title(ctx.text(title), ctx.text(subtitle), fadeIn, stay, fadeOut);
        }));
        registry.register(simple("sound", json -> {
            String sound = requireString(json, "sound");
            float volume = (float) optDouble(json, "volume", 1.0);
            float pitch = (float) optDouble(json, "pitch", 1.0);
            return ctx -> ctx.host().playSound(sound, volume, pitch);
        }));
        registry.register(simple("command", json -> {
            String command = requireString(json, "command");
            String stripped = command.startsWith("/") ? command.substring(1) : command;
            return ctx -> ctx.host().runCommand(ctx.fill(stripped));
        }));
        return registry;
    }

    public void register(EventActionType type) {
        if (types.putIfAbsent(type.id(), type) != null) {
            throw new IllegalArgumentException("Aktionstyp bereits registriert: " + type.id());
        }
    }

    public Set<String> typeIds() {
        return types.keySet();
    }

    public EventAction parse(JsonElement element) throws EventConfigException {
        if (element == null || !element.isJsonObject()) {
            throw new EventConfigException("Aktion muss ein Objekt mit \"type\" sein");
        }
        JsonObject json = element.getAsJsonObject();
        String typeId = requireString(json, "type");
        EventActionType type = types.get(typeId);
        if (type == null) {
            throw new EventConfigException("Unbekannter Aktionstyp '" + typeId + "' (bekannt: "
                    + String.join(", ", types.keySet()) + ")");
        }
        return type.parse(json);
    }

    @FunctionalInterface
    public interface Parser {
        EventAction parse(JsonObject json) throws EventConfigException;
    }

    public static EventActionType simple(String id, Parser parser) {
        return new EventActionType() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public EventAction parse(JsonObject json) throws EventConfigException {
                return parser.parse(json);
            }
        };
    }

    public static String requireString(JsonObject json, String key) throws EventConfigException {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || value.getAsString().isBlank()) {
            throw new EventConfigException("'" + key + "' fehlt");
        }
        return value.getAsString();
    }

    public static String optString(JsonObject json, String key, String fallback) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }

    public static int optInt(JsonObject json, String key, int fallback) throws EventConfigException {
        return (int) optDouble(json, key, fallback);
    }

    public static double optDouble(JsonObject json, String key, double fallback) throws EventConfigException {
        JsonElement value = json.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            return value.getAsDouble();
        } catch (RuntimeException e) {
            throw new EventConfigException("'" + key + "' muss eine Zahl sein");
        }
    }
}
