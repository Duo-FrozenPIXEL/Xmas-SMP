package de.xmassmp.crystal;

import de.xmassmp.MutableClock;
import de.xmassmp.TestPlatform;
import de.xmassmp.TestPlatform.FakePlayer;
import de.xmassmp.XmasCore;
import de.xmassmp.item.ItemCatalog;
import de.xmassmp.item.ItemSpec;
import de.xmassmp.player.PlayerData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gebundene Kristall-Items: Vergabe beim Betreten, Abgleich, Seelenbindung. */
class CrystalServiceTest {

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-12-20T12:00:00Z"));
    private TestPlatform platform;
    private XmasCore core;

    @BeforeEach
    void setUp() {
        platform = new TestPlatform(dir);
        core = new XmasCore(platform, clock);
        core.start();
    }

    private FakePlayer join(String name) {
        FakePlayer player = platform.join(name);
        core.onPlayerJoin(player);
        return player;
    }

    private void tickSeconds(int seconds) {
        for (int i = 0; i < seconds * 20; i++) {
            core.tick();
        }
    }

    private void editConfig(String from, String to) throws IOException {
        Path config = dir.resolve("config.jsonc");
        String text = Files.readString(config);
        assertTrue(text.contains(from), from);
        Files.writeString(config, text.replace(from, to));
        core.reload();
    }

    @Test
    void firstJoinAssignsBoundCrystal() {
        FakePlayer steve = join("Steve");
        PlayerData data = core.players().get(steve.uuid());
        assertTrue(core.crystals().get(data.crystalId()).isPresent());

        assertEquals(1, steve.crystals().size());
        ItemSpec item = steve.crystals().get(0);
        assertEquals(steve.uuid().toString(), item.data().get(ItemCatalog.DATA_OWNER));
        assertEquals(data.crystalId(), item.data().get("crystal"));
        assertEquals("5/10", item.data().get("energy"));
        assertTrue(item.lore().stream().anyMatch(l -> l.toPlainString().equals("Seelengebunden")));
        assertTrue(steve.messages.get(0).contains("erhalten"));

        core.tick();
        assertTrue(platform.effects.contains("s:minecraft:block.amethyst_block.resonate"));
    }

    @Test
    void existingCrystalIsKeptAndNotReplacedNeedlessly() {
        FakePlayer steve = platform.join("Steve");
        core.players().get(steve.uuid()).setCrystalId("wind");
        core.onPlayerJoin(steve);
        ItemSpec first = steve.crystals().get(0);
        assertEquals("wind", first.data().get("crystal"));
        assertTrue(steve.messages.isEmpty());

        tickSeconds(2);
        assertSame(first, steve.crystals().get(0));
    }

    @Test
    void onlyEnabledCrystalsAreAssigned() throws IOException {
        for (String id : new String[] {"fire", "earth", "wind", "shadow"}) {
            editConfig("\"" + id + "\":" + " ".repeat(6 - id.length()) + " { \"enabled\": true",
                    "\"" + id + "\":" + " ".repeat(6 - id.length()) + " { \"enabled\": false");
        }
        for (int i = 0; i < 10; i++) {
            FakePlayer player = join("P" + i);
            assertEquals("nature", core.players().get(player.uuid()).crystalId());
        }
    }

    @Test
    void giveOnJoinCanBeDisabled() throws IOException {
        editConfig("\"giveOnJoin\": true", "\"giveOnJoin\": false");
        FakePlayer steve = join("Steve");
        assertNull(core.players().get(steve.uuid()).crystalId());
        assertTrue(steve.crystals().isEmpty());
    }

    @Test
    void unknownCrystalIsReplaced() {
        FakePlayer steve = platform.join("Steve");
        core.players().get(steve.uuid()).setCrystalId("ice");
        core.onPlayerJoin(steve);
        assertTrue(core.crystals().get(core.players().get(steve.uuid()).crystalId()).isPresent());
    }

    @Test
    void adminChangesUpdateItemInPlace() {
        FakePlayer steve = join("Steve");
        steve.inventory.add(0, ItemCatalog.custom(de.xmassmp.item.CustomItem.GEM_FRAGMENT));
        core.admin().setCrystal("Admin", steve.uuid(), "Steve", "shadow");
        core.admin().setTier("Admin", steve.uuid(), "Steve", 2);
        core.admin().setEnergy("Admin", steve.uuid(), "Steve", 7);

        ItemSpec item = steve.crystals().get(0);
        assertEquals("shadow", item.data().get("crystal"));
        assertEquals("2", item.data().get("tier"));
        assertEquals("7/10", item.data().get("energy"));
        assertEquals(CrystalModels.customModelData(Crystals.SHADOW, 2, PristineStage.PRISTINE), item.customModelData());
        assertEquals(2, steve.inventory.size());
    }

    @Test
    void soulboundCrystalComesBackAndCopiesVanish() {
        FakePlayer steve = join("Steve");
        FakePlayer alex = join("Alex");
        ItemSpec own = steve.crystals().get(0);

        // Gedroppt
        steve.inventory.remove(own);
        steve.dropped.add(own);
        tickSeconds(1);
        assertTrue(steve.dropped.isEmpty());
        assertEquals(1, steve.crystals().size());

        // Kopie, fremder Kristall, Kopie im Behälter
        steve.inventory.add(own);
        steve.inventory.add(alex.crystals().get(0));
        steve.openContainer.add(own);
        // Ungebundener Admin-Kristall bleibt
        ItemSpec unbound = ItemCatalog.crystal(Crystals.FIRE, 1, PristineStage.PRISTINE);
        steve.inventory.add(unbound);
        tickSeconds(1);
        assertEquals(2, steve.crystals().size());
        assertTrue(steve.crystals().contains(own));
        assertTrue(steve.crystals().contains(unbound));
        assertTrue(steve.openContainer.isEmpty());
    }

    @Test
    void deathRemovesDroppedCrystalAndRespawnRestoresIt() {
        FakePlayer steve = join("Steve");
        ItemSpec own = steve.crystals().get(0);
        steve.inventory.clear();
        steve.dropped.add(own);
        core.onPlayerDeath(steve, null);
        assertTrue(steve.dropped.isEmpty());
        core.onPlayerRespawn(steve);
        assertEquals(1, steve.crystals().size());
    }

    @Test
    void fullInventoryRetriesLater() {
        FakePlayer steve = join("Steve");
        steve.inventory.clear();
        steve.capacity = 0;
        tickSeconds(1);
        assertTrue(steve.crystals().isEmpty());
        steve.capacity = 36;
        tickSeconds(1);
        assertEquals(1, steve.crystals().size());
    }

    @Test
    void notSoulboundOnlyUpdatesAppearance() throws IOException {
        editConfig("\"soulbound\": true", "\"soulbound\": false");
        FakePlayer steve = join("Steve");
        ItemSpec own = steve.crystals().get(0);
        assertTrue(own.lore().stream().noneMatch(l -> l.toPlainString().equals("Seelengebunden")));

        steve.inventory.remove(own);
        steve.dropped.add(own);
        tickSeconds(1);
        assertTrue(steve.crystals().isEmpty(), "verlorener Kristall kommt nicht zurück");
        assertEquals(1, steve.dropped.size());
        assertTrue(core.players().get(steve.uuid()).crystalId() != null, "Kraft bleibt");

        steve.inventory.add(own);
        core.admin().setTier("Admin", steve.uuid(), "Steve", 2);
        assertEquals("2", steve.crystals().get(0).data().get("tier"));
    }

    @Test
    void offlinePlayerIsSyncedOnJoin() {
        UUID offline = UUID.nameUUIDFromBytes("Notch".getBytes());
        core.admin().setCrystal("Admin", offline, "Notch", "earth");
        FakePlayer notch = join("Notch");
        assertEquals("earth", notch.crystals().get(0).data().get("crystal"));
    }
}
