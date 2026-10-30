package de.xmassmp.player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import de.xmassmp.crystal.CrystalModels;
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
 * Cache, bis sie mit {@link #unload(UUID)} entfernt werden. Daten, die nicht gespeichert werden
 * konnten, bleiben im Cache und werden beim nächsten Autosave erneut geschrieben.
 */
public final class PlayerDataStore {

    private final Path directory;
    private final IntSupplier startEnergy;
    private final IntSupplier minEnergy;
    private final IntSupplier maxEnergy;
    private final XmasLogger logger;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    /** Ohne Energie-Grenzen (Tests); der Kern nutzt die Variante mit {@code energy.min/max}. */
    public PlayerDataStore(Path dataDirectory, IntSupplier startEnergy, XmasLogger logger) {
        this(dataDirectory, startEnergy, () -> Integer.MIN_VALUE, () -> Integer.MAX_VALUE, logger);
    }

    public PlayerDataStore(Path dataDirectory, IntSupplier startEnergy, IntSupplier minEnergy, IntSupplier maxEnergy,
                           XmasLogger logger) {
        this.directory = dataDirectory.resolve("playerdata");
        this.startEnergy = startEnergy;
        this.minEnergy = minEnergy;
        this.maxEnergy = maxEnergy;
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

    /** Entfernt den Spieler aus dem Cache – nur, wenn seine Änderungen gespeichert sind. */
    public void unload(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) {
            return;
        }
        if (data.isDirty() && !write(data)) {
            logger.warn("Spielerdaten von " + uuid + " bleiben im Speicher und werden beim nächsten Autosave "
                    + "erneut gespeichert");
            return;
        }
        cache.remove(uuid);
    }

    /** Beim Stoppen: speichert alles, mit einem zweiten Versuch; was dann noch fehlt, landet im Log. */
    public void saveAndUnloadAll() {
        for (PlayerData data : cache.values()) {
            if (data.isDirty() && !write(data) && !write(data)) {
                logger.error("Spielerdaten von " + data.uuid() + " gingen beim Stoppen verloren, letzter Stand: "
                        + JsonFiles.GSON.toJson(toJson(data)), null);
            }
        }
        cache.clear();
    }

    /** Begrenzt die Energie geladener Spieler, z. B. nach einer Änderung von {@code energy.min/max}. */
    public void clampLoaded() {
        cache.values().forEach(this::clamp);
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
            PlayerData data = new PlayerData(uuid, crystal, tier, energy, cooldowns);
            clamp(data);
            return data;
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

    /** Setzt unplausible Werte (Handbearbeitung, geänderte Grenzen) in den gültigen Bereich. */
    private void clamp(PlayerData data) {
        if (!CrystalModels.isValidTier(data.tier())) {
            int fixed = Math.max(CrystalModels.MIN_TIER, Math.min(CrystalModels.MAX_TIER, data.tier()));
            logger.warn("Spielerdaten von " + data.uuid() + ": Tier " + data.tier() + " ungültig – auf " + fixed
                    + " gesetzt");
            data.setTier(fixed);
        }
        int min = minEnergy.getAsInt();
        int max = maxEnergy.getAsInt();
        if (min <= max && (data.energy() < min || data.energy() > max)) {
            int fixed = Math.max(min, Math.min(max, data.energy()));
            logger.warn("Spielerdaten von " + data.uuid() + ": Energie " + data.energy() + " außerhalb von " + min
                    + "–" + max + " – auf " + fixed + " gesetzt");
            data.setEnergy(fixed);
        }
    }

    /** @return {@code true}, wenn gespeichert */
    private boolean write(PlayerData data) {
        try {
            JsonFiles.writeAtomic(file(data.uuid()), JsonFiles.GSON.toJson(toJson(data)));
            data.markClean();
            return true;
        } catch (IOException | JsonParseException e) {
            logger.error("Spielerdaten von " + data.uuid() + " konnten nicht gespeichert werden", e);
            return false;
        }
    }

    private static JsonObject toJson(PlayerData data) {
        JsonObject json = new JsonObject();
        json.addProperty("schema", PlayerData.SCHEMA_VERSION);
        json.addProperty("crystal", data.crystalId());
        json.addProperty("tier", data.tier());
        json.addProperty("energy", data.energy());
        JsonObject cooldowns = new JsonObject();
        data.cooldowns().forEach(cooldowns::addProperty);
        json.add("cooldowns", cooldowns);
        return json;
    }
}
