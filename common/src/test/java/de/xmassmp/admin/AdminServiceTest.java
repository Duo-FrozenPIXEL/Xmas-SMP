package de.xmassmp.admin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.xmassmp.MutableClock;
import de.xmassmp.XmasCore;
import de.xmassmp.effect.EffectSink;
import de.xmassmp.effect.ParticleSpec;
import de.xmassmp.event.EventHost;
import de.xmassmp.item.ItemSpec;
import de.xmassmp.platform.Platform;
import de.xmassmp.platform.WorldRef;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.util.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testet die Admin-Befehle über den kompletten Kern. */
class AdminServiceTest {

    @TempDir
    Path dir;

    private final UUID steve = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private final List<ItemSpec> given = new ArrayList<>();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-12-20T12:00:00Z"));
    private XmasCore core;
    private AdminService admin;

    @BeforeEach
    void setUp() {
        core = new XmasCore(new TestPlatform(dir), clock);
        core.start();
        admin = core.admin();
    }

    @Test
    void giveCrystalBuildsSpecAndAudits() throws IOException {
        AdminService.Result result = admin.giveCrystal("Admin", "Steve", "fire", 2, "pristine", given::add);
        assertTrue(result.success(), result.message());
        assertEquals(1, given.size());
        assertEquals(2047, given.get(0).customModelData());
        assertEquals("fire", given.get(0).data().get("crystal"));
        assertEquals("Feuer-Kristall", given.get(0).name().toPlainString());

        List<String> lines = Files.readAllLines(core.audit().file());
        assertEquals(1, lines.size());
        JsonObject entry = JsonParser.parseString(lines.get(0)).getAsJsonObject();
        assertEquals("Admin", entry.get("actor").getAsString());
        assertEquals("give", entry.get("action").getAsString());
        assertEquals("Steve", entry.get("target").getAsString());
        assertEquals("2026-12-20T12:00:00Z", entry.get("time").getAsString());
    }

    @Test
    void giveRejectsInvalidInputWithoutAudit() {
        assertFalse(admin.giveCrystal("Admin", "Steve", "ice", 1, "pristine", given::add).success());
        assertFalse(admin.giveCrystal("Admin", "Steve", "fire", 3, "pristine", given::add).success());
        assertFalse(admin.giveCrystal("Admin", "Steve", "fire", 1, "shiny", given::add).success());
        assertFalse(admin.giveItem("Admin", "Steve", "cookie", given::add).success());
        assertTrue(given.isEmpty());
        assertFalse(Files.exists(core.audit().file()));
    }

    @Test
    void giveCustomItem() {
        assertTrue(admin.giveItem("Admin", "Steve", "gem_trader", given::add).success());
        assertEquals("minecraft:emerald", given.get(0).baseItem());
        assertEquals(4002, given.get(0).customModelData());
    }

    @Test
    void setCommandsValidateAndPersist() {
        assertTrue(admin.setCrystal("Admin", steve, "Steve", "nature").success());
        assertTrue(admin.setTier("Admin", steve, "Steve", 2).success());
        assertTrue(admin.setEnergy("Admin", steve, "Steve", 10).success());
        assertFalse(admin.setEnergy("Admin", steve, "Steve", 11).success());
        assertFalse(admin.setTier("Admin", steve, "Steve", 0).success());
        assertFalse(admin.setCrystal("Admin", steve, "Steve", "ice").success());

        core.shutdown();
        XmasCore reloaded = new XmasCore(new TestPlatform(dir), clock);
        var data = reloaded.players().get(steve);
        assertEquals("nature", data.crystalId());
        assertEquals(2, data.tier());
        assertEquals(10, data.energy());
    }

    @Test
    void auditCanBeDisabled() throws IOException {
        Path config = dir.resolve("config.jsonc");
        Files.writeString(config, Files.readString(config).replace("\"enabled\": true\n  }\n}", "\"enabled\": false\n  }\n}"));
        admin.reload("Admin");
        assertFalse(core.config().get().audit.enabled);
        admin.setTier("Admin", steve, "Steve", 2);
        assertFalse(Files.exists(core.audit().file()));
    }

    @Test
    void eventCommands() {
        List<String> list = admin.eventList();
        assertTrue(list.get(0).contains("Europe/Berlin"));
        assertTrue(list.stream().anyMatch(l -> l.contains("xmas_eve") && l.contains("deaktiviert")));

        assertTrue(admin.eventStart("Admin", "xmas_eve").success());
        assertFalse(admin.eventStart("Admin", "xmas_eve").success());
        assertEquals(List.of("xmas_eve"), admin.runningEventIds());
        assertTrue(admin.eventList().stream().anyMatch(l -> l.contains("xmas_eve") && l.contains("läuft")));
        assertTrue(admin.eventStop("Admin", "xmas_eve").success());
        assertFalse(admin.eventStop("Admin", "xmas_eve").success());
        assertFalse(admin.eventStart("Admin", "gibts_nicht").success());
    }

    @Test
    void simulateParsesDateAndTime() {
        assertFalse(admin.eventSimulate("Admin", "24.12.2026", "18:00", true).success());
        assertTrue(admin.eventSimulate("Admin", "2026-12-24", "17:59", true).success());
        assertTrue(core.events().isSimulating());
        assertTrue(admin.eventList().get(0).contains("Testmodus"));
        assertTrue(admin.eventSimulateOff("Admin").success());
        assertFalse(admin.eventSimulateOff("Admin").success());
    }

    @Test
    void suggestions() {
        assertEquals(List.of("fire", "earth", "wind", "shadow", "nature"), admin.crystalIds());
        assertTrue(AdminService.itemIds().contains("energy_bottle"));
        assertEquals(List.of("broken", "damaged", "scratched", "pristine"), AdminService.stageIds());
        assertEquals(4, admin.eventIds().size());
        assertTrue(admin.simulateDateSuggestions().contains("2026-12-24"));
    }

    private record TestPlatform(Path dir) implements Platform {
        @Override
        public Path configDirectory() {
            return dir;
        }

        @Override
        public Path dataDirectory() {
            return dir.resolve("data");
        }

        @Override
        public XmasLogger logger() {
            return XmasLogger.NOOP;
        }

        @Override
        public EffectSink effectSink() {
            return new EffectSink() {
                @Override
                public void particle(WorldRef world, Vec3 position, ParticleSpec spec, int count) {
                }

                @Override
                public void sound(WorldRef world, Vec3 position, String sound, float volume, float pitch) {
                }
            };
        }

        @Override
        public EventHost eventHost() {
            return new EventHost() {
                @Override
                public void broadcast(String legacyText) {
                }

                @Override
                public void title(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
                }

                @Override
                public void playSound(String sound, float volume, float pitch) {
                }

                @Override
                public void runCommand(String command) {
                }
            };
        }
    }
}
