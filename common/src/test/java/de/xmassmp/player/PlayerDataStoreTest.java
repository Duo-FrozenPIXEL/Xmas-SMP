package de.xmassmp.player;

import de.xmassmp.platform.XmasLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDataStoreTest {

    @TempDir
    Path dir;

    private final UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void newPlayerGetsDefaults() {
        PlayerData data = new PlayerDataStore(dir, () -> 7, XmasLogger.NOOP).get(uuid);
        assertNull(data.crystalId());
        assertEquals(1, data.tier());
        assertEquals(7, data.energy());
    }

    @Test
    void dataSurvivesSaveAndReload() {
        PlayerDataStore store = new PlayerDataStore(dir, () -> 5, XmasLogger.NOOP);
        PlayerData data = store.get(uuid);
        data.setCrystalId("shadow");
        data.setTier(2);
        data.setEnergy(9);
        data.setCooldownUntil("shadow.step", 50_000);
        store.unload(uuid);
        assertFalse(store.isLoaded(uuid));

        PlayerData loaded = new PlayerDataStore(dir, () -> 5, XmasLogger.NOOP).get(uuid);
        assertEquals("shadow", loaded.crystalId());
        assertEquals(2, loaded.tier());
        assertEquals(9, loaded.energy());
        assertEquals(10_000, loaded.remainingCooldownMillis("shadow.step", 40_000));
    }

    @Test
    void cooldownsArePruned() {
        PlayerData data = new PlayerData(uuid, 5);
        data.setCooldownUntil("a", 100);
        data.setCooldownUntil("b", 300);
        data.pruneCooldowns(200);
        assertFalse(data.isOnCooldown("a", 0));
        assertTrue(data.isOnCooldown("b", 200));
        assertEquals(1, data.cooldowns().size());
    }

    @Test
    void brokenFileIsBackedUpNotLost() throws IOException {
        PlayerDataStore store = new PlayerDataStore(dir, () -> 5, XmasLogger.NOOP);
        Path file = store.file(uuid);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{ kaputt");

        PlayerData data = store.get(uuid);
        assertEquals(5, data.energy());
        assertFalse(Files.exists(file));
        try (var files = Files.list(file.getParent())) {
            assertTrue(files.anyMatch(p -> p.getFileName().toString().contains(".broken-")));
        }
    }

    @Test
    void saveDirtyOnlyWritesChangedData() {
        PlayerDataStore store = new PlayerDataStore(dir, () -> 5, XmasLogger.NOOP);
        store.get(uuid);
        store.saveDirty();
        assertTrue(Files.exists(store.file(uuid)));
        PlayerData data = store.get(uuid);
        assertFalse(data.isDirty());
        data.setEnergy(3);
        assertTrue(data.isDirty());
        store.saveDirty();
        assertFalse(data.isDirty());
    }
}
