package de.xmassmp.config;

import de.xmassmp.effect.EffectQuality;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {

    @TempDir
    Path dir;

    @Test
    void templateIsWrittenAndMatchesDefaults() {
        ConfigManager manager = new ConfigManager(dir);
        ConfigManager.LoadResult result = manager.load();
        assertTrue(result.success());
        assertEquals(1, result.messages().size(), "nur 'angelegt', keine Warnungen: " + result.messages());
        assertTrue(Files.exists(dir.resolve(ConfigManager.FILE_NAME)));

        XmasConfig cfg = manager.get();
        XmasConfig defaults = new XmasConfig();
        assertEquals(defaults.energy.max, cfg.energy.max);
        assertEquals(defaults.effects.maxParticlesPerTick, cfg.effects.maxParticlesPerTick);
        assertEquals("Europe/Berlin", cfg.events.timezone);
        assertEquals(5, cfg.crystals.size());
        assertTrue(cfg.crystal("fire").enabled);
    }

    @Test
    void missingFieldsKeepDefaultsAndCommentsAreAllowed() throws IOException {
        Files.writeString(dir.resolve(ConfigManager.FILE_NAME), """
                // Kommentar
                { "effects": { "quality": "LOW" } # noch einer
                }
                """);
        ConfigManager manager = new ConfigManager(dir);
        assertTrue(manager.load().success());
        assertEquals(EffectQuality.LOW, manager.get().effects.quality);
        assertEquals(3000, manager.get().effects.maxParticlesPerTick);
        assertTrue(manager.get().general.enabled);
        assertEquals(1.0, manager.get().crystal("unbekannt").cooldownMultiplier);
    }

    @Test
    void invalidValuesAreCorrectedWithWarnings() throws IOException {
        Files.writeString(dir.resolve(ConfigManager.FILE_NAME), """
                { "energy": { "start": 50, "min": 10, "max": 0 },
                  "effects": { "quality": "ULTRA", "maxParticlesPerTick": 5 },
                  "events": { "timezone": "Mars/Olympus" },
                  "crystals": { "fire": { "cooldownMultiplier": -1 } } }
                """);
        ConfigManager manager = new ConfigManager(dir);
        ConfigManager.LoadResult result = manager.load();
        assertTrue(result.success());
        XmasConfig cfg = manager.get();
        assertEquals(0, cfg.energy.min);
        assertEquals(10, cfg.energy.max);
        assertEquals(10, cfg.energy.start);
        assertEquals(EffectQuality.HIGH, cfg.effects.quality);
        assertEquals(100, cfg.effects.maxParticlesPerTick);
        assertEquals("Europe/Berlin", cfg.events.timezone);
        assertEquals(1.0, cfg.crystal("fire").cooldownMultiplier);
        assertEquals(6, result.messages().size(), result.messages().toString());
    }

    @Test
    void nullCrystalEntryGetsDefaults() throws IOException {
        Files.writeString(dir.resolve(ConfigManager.FILE_NAME), """
                { "crystals": { "fire": null, "ice": { "cooldownMultiplier": 2 } } }
                """);
        ConfigManager manager = new ConfigManager(dir);
        ConfigManager.LoadResult result = manager.load();
        assertTrue(result.success());
        assertEquals(1, result.messages().size(), result.messages().toString());
        assertTrue(result.messages().get(0).contains("crystals.fire"));
        assertEquals(1.0, manager.get().crystal("fire").cooldownMultiplier);
        assertTrue(manager.get().crystal("fire").enabled);
        assertEquals(2.0, manager.get().crystal("ice").cooldownMultiplier);
    }

    @Test
    void nullSectionsAndValuesDoNotThrow() throws IOException {
        Files.writeString(dir.resolve(ConfigManager.FILE_NAME), """
                { "general": null, "energy": null, "effects": { "quality": null },
                  "events": { "timezone": null }, "audit": null }
                """);
        ConfigManager manager = new ConfigManager(dir);
        assertTrue(manager.load().success());
        assertEquals(EffectQuality.HIGH, manager.get().effects.quality);
        assertEquals("Europe/Berlin", manager.get().events.timezone);
        assertTrue(manager.get().general.enabled);
    }

    @Test
    void brokenFileKeepsPreviousConfig() throws IOException {
        ConfigManager manager = new ConfigManager(dir);
        AtomicInteger reloads = new AtomicInteger();
        manager.onReload(cfg -> reloads.incrementAndGet());
        manager.load();
        XmasConfig before = manager.get();

        Files.writeString(dir.resolve(ConfigManager.FILE_NAME), "{ \"energy\": { \"max\": ");
        ConfigManager.LoadResult result = manager.load();
        assertFalse(result.success());
        assertTrue(result.messages().get(0).contains("alte Konfiguration bleibt aktiv"));
        assertEquals(before, manager.get());
        assertEquals(1, reloads.get());
    }
}
