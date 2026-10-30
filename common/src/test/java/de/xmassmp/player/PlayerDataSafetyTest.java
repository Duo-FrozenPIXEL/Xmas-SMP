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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plausibilitätsprüfung beim Laden und kein Datenverlust bei Schreibfehlern (bug.md Nr. 13, 14). */
class PlayerDataSafetyTest {

    @TempDir
    Path dir;

    private final UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void invalidValuesAreClampedOnLoad() throws IOException {
        Path folder = Files.createDirectories(dir.resolve("playerdata"));
        Files.writeString(folder.resolve(uuid + ".json"), "{ \"crystal\": \"fire\", \"tier\": 3, \"energy\": 50 }");
        PlayerDataStore store = new PlayerDataStore(dir, () -> 5, () -> 0, () -> 10, XmasLogger.NOOP);
        PlayerData data = store.get(uuid);
        assertEquals(2, data.tier());
        assertEquals(10, data.energy());
        assertTrue(data.isDirty(), "korrigierte Werte werden gespeichert");
    }

    @Test
    void changedEnergyBoundsApplyToLoadedPlayers() {
        int[] max = {10};
        PlayerDataStore store = new PlayerDataStore(dir, () -> 5, () -> 0, () -> max[0], XmasLogger.NOOP);
        store.get(uuid).setEnergy(9);
        max[0] = 6;
        store.clampLoaded();
        assertEquals(6, store.get(uuid).energy());
    }

    @Test
    void failedSaveKeepsDataInMemory() throws IOException {
        // Eine Datei an Stelle des Ordners lässt jedes Speichern scheitern.
        Files.writeString(dir.resolve("playerdata"), "blockiert");
        PlayerDataStore store = new PlayerDataStore(dir, () -> 5, XmasLogger.NOOP);
        store.get(uuid).setEnergy(8);
        store.unload(uuid);
        assertTrue(store.isLoaded(uuid), "nicht gespeichert, also nicht verworfen");

        Files.delete(dir.resolve("playerdata"));
        store.saveDirty();
        assertFalse(store.get(uuid).isDirty());
        store.unload(uuid);
        assertFalse(store.isLoaded(uuid));
        assertEquals(8, new PlayerDataStore(dir, () -> 5, XmasLogger.NOOP).get(uuid).energy());
    }
}
