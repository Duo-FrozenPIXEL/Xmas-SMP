package de.xmassmp.player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.util.JsonFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;

/**
 * Speichert {@link PlayerData} als {@code playerdata/<uuid>.json}. Geladene Spieler bleiben im
 * Cache, bis sie mit {@link #unload(UUID)} entfernt werden.
 */
public final class PlayerDataStore {

    private final Path directory;
    private final IntSupplier startEnergy;
    private final XmasLogger logger;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataStore(Path dataDirectory, IntSupplier startEnergy, XmasLogger logger) {
        this.directory = dataDirectory.resolve("playerdata");
        this.startEnergy = startEnergy;
        this.logger = logger;
    }

    /** Lädt aus dem Cache oder von der Platte; legt neue Daten an, wenn keine existieren. */
    public PlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::read);
    }

    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public void save(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            write(data);
        }
    }

    /** Speichert nur geänderte Daten. */
    public void saveDirty() {
        cache.values().stream().filter(PlayerData::isDirty).forEach(this::write);
    }

    public void unload(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null && data.isDirty()) {
            write(data);
        }
    }

    public void saveAndUnloadAll() {
        saveDirty();
        cache.clear();
    }

    Path file(UUID uuid) {
        return directory.resolve(uuid + ".json");
    }

    private PlayerData read(UUID uuid) {
        Path file = file(uuid);
        if (Files.notExists(file)) {
            return new PlayerData(uuid, startEnergy.getAsInt());
        }
        try {
            JsonObject json = JsonFiles.parseLenient(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            String crystal = json.has("crystal") && !json.get("crystal").isJsonNull()
                    ? json.get("crystal").getAsString() : null;
            int tier = json.has("tier") ? json.get("tier").getAsInt() : 1;
            int energy = json.has("energy") ? json.get("energy").getAsInt() : startEnergy.getAsInt();
            Map<String, Long> cooldowns = new HashMap<>();
            if (json.has("cooldowns")) {
                json.getAsJsonObject("cooldowns").entrySet()
                        .forEach(e -> cooldowns.put(e.getKey(), e.getValue().getAsLong()));
            }
            return new PlayerData(uuid, crystal, tier, energy, cooldowns);
        } catch (IOException | RuntimeException e) {
            // Defekte Datei sichern statt überschreiben, damit nichts verloren geht.
            Path backup = file.resolveSibling(uuid + ".broken-" + System.currentTimeMillis() + ".json");
            logger.error("Spielerdaten " + file + " defekt, gesichert als " + backup.getFileName(), e);
            try {
                Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveError) {
                logger.error("Sicherung von " + file + " fehlgeschlagen", moveError);
            }
            return new PlayerData(uuid, startEnergy.getAsInt());
        }
    }

    private void write(PlayerData data) {
        JsonObject json = new JsonObject();
        json.addProperty("schema", PlayerData.SCHEMA_VERSION);
        json.addProperty("crystal", data.crystalId());
        json.addProperty("tier", data.tier());
        json.addProperty("energy", data.energy());
        JsonObject cooldowns = new JsonObject();
        data.cooldowns().forEach(cooldowns::addProperty);
        json.add("cooldowns", cooldowns);
        try {
            JsonFiles.writeAtomic(file(data.uuid()), JsonFiles.GSON.toJson(json));
            data.markClean();
        } catch (IOException | JsonParseException e) {
            logger.error("Spielerdaten von " + data.uuid() + " konnten nicht gespeichert werden", e);
        }
    }
}
