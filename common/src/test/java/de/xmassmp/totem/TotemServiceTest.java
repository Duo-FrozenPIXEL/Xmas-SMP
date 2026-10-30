package de.xmassmp.totem;

import de.xmassmp.MutableClock;
import de.xmassmp.TestPlatform;
import de.xmassmp.TestPlatform.FakePlayer;
import de.xmassmp.XmasCore;
import de.xmassmp.energy.EnergyLedger;
import de.xmassmp.item.CustomItem;
import de.xmassmp.item.ItemCatalog;
import de.xmassmp.util.BlockPos;
import de.xmassmp.util.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Repair-Totem: Aufstellen, Teilnahme, Ausscheiden, Zerstören, Ende mit Energie, Persistenz. */
class TotemServiceTest {

    private static final BlockPos POS = new BlockPos(3, 64, 3);
    private static final String COORDS = "3 64 3 (overworld)";
    private static final Vec3 FAR_AWAY = new Vec3(100, 64, 0);
    /** Im Test dauert ein Totem 1 Minute; {@code joinSeconds} bleibt 30. */
    private static final int DURATION_TICKS = 60 * 20;
    private static final int JOIN_TICKS = 30 * 20;

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-12-20T12:00:00Z"));
    private TestPlatform platform;
    private XmasCore core;
    private FakePlayer steve;
    private FakePlayer alex;
    private FakePlayer notch;

    @BeforeEach
    void setUp() {
        platform = new TestPlatform(dir);
        core = startCore();
        steve = join("Steve", "fire", 2);
        alex = join("Alex", "wind", 7);
        notch = join("Notch", "earth", 1);
        notch.position = FAR_AWAY;
    }

    private XmasCore startCore() {
        XmasCore started = new XmasCore(platform, clock);
        started.start();
        started.config().get().totem.durationMinutes = 1;
        return started;
    }

    private FakePlayer join(String name, String crystal, int energy) {
        FakePlayer player = platform.join(name);
        core.players().get(player.uuid()).setCrystalId(crystal);
        core.players().get(player.uuid()).setEnergy(energy);
        core.onPlayerJoin(player);
        return player;
    }

    private int energy(FakePlayer player) {
        return core.players().get(player.uuid()).energy();
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            core.tick();
        }
    }

    private boolean place(FakePlayer player) {
        player.hold(ItemCatalog.custom(CustomItem.REPAIR_KIT), 1);
        return core.totem().tryPlace(player, TestPlatform.WORLD, POS);
    }

    private static boolean got(FakePlayer player, String text) {
        return player.messages.stream().anyMatch(m -> m.contains(text));
    }

    // ------------------------------------------------------------------ Aufstellen

    @Test
    void placingConsumesKitAndAnnouncesCoordinatesToEveryone() {
        assertTrue(place(steve));
        assertTrue(core.totem().isActive());
        assertNull(steve.mainHand, "Repair Kit verbraucht");
        assertTrue(platform.totems.contains("overworld@" + POS), "Totem-Block gesetzt");
        for (FakePlayer player : new FakePlayer[]{steve, alex, notch}) {
            assertTrue(player.titles.contains("§d✚ Repair-Totem|§f" + COORDS), player.name() + ": " + player.titles);
            assertTrue(got(player, COORDS), player.name() + ": " + player.messages);
        }
        assertTrue(core.totem().status().orElseThrow().contains(COORDS));
    }

    @Test
    void secondTotemIsRejectedAndKeepsKit() {
        place(steve);
        assertTrue(place(alex));
        assertEquals(1, alex.mainHandCount, "Kit bleibt");
        assertTrue(alex.lastActionBar().contains("schon ein Totem"), alex.actionBar.toString());
        assertEquals(1, platform.totems.size());

        // Auch an einer anderen Stelle
        alex.hold(ItemCatalog.custom(CustomItem.REPAIR_KIT), 1);
        assertTrue(core.totem().tryPlace(alex, TestPlatform.WORLD, new BlockPos(50, 64, 50)));
        assertEquals(1, platform.totems.size());
        assertEquals(1, alex.mainHandCount);
    }

    @Test
    void blockedSpotKeepsKit() {
        platform.blocked.add("overworld@" + POS);
        assertTrue(place(steve));
        assertFalse(core.totem().isActive());
        assertEquals(1, steve.mainHandCount);
        assertTrue(steve.lastActionBar().contains("nicht stehen"), steve.actionBar.toString());
    }

    @Test
    void otherItemsAreNotHandled() {
        steve.hold(ItemCatalog.custom(CustomItem.UPGRADER), 1);
        assertFalse(core.totem().tryPlace(steve, TestPlatform.WORLD, POS));
        assertFalse(core.totem().isActive());
    }

    // ------------------------------------------------------------------ Ablauf

    @Test
    void participantsWhoStayUntilTheEndAreRestoredToFive() {
        place(steve);
        ticks(DURATION_TICKS - 1);
        assertTrue(core.totem().isActive(), "kurz vor Ende läuft das Totem noch");
        assertEquals(2, energy(steve), "Energie erst am Ende");
        assertTrue(got(steve, "nimmst am Repair-Totem teil"), steve.messages.toString());

        ticks(1);
        assertFalse(core.totem().isActive());
        assertTrue(platform.totems.isEmpty(), "Totem-Block entfernt");
        assertEquals(5, energy(steve), "2 → 5");
        assertEquals(7, energy(alex), "7 bleibt 7");
        assertTrue(got(alex, "schon genug Energie"), alex.messages.toString());
        assertEquals(1, energy(notch), "außerhalb des Kreises: nichts");
        assertFalse(got(notch, "nimmst am Repair-Totem teil"));

        var totemEntries = core.ledger().recent(steve.uuid(), 20).stream()
                .filter(e -> EnergyLedger.TOTEM.equals(e.reason())).toList();
        assertEquals(1, totemEntries.size());
        assertEquals(2, totemEntries.get(0).from());
        assertEquals(5, totemEntries.get(0).to());
        assertTrue(steve.titles.stream().anyMatch(t -> t.contains("5 Energie")), steve.titles.toString());

        // Danach darf wieder ein Totem stehen
        assertTrue(place(alex));
        assertTrue(core.totem().isActive());
    }

    @Test
    void lateArrivalsDoNotJoin() {
        place(steve);
        ticks(JOIN_TICKS + 20);
        notch.position = new Vec3(0, 64, 0);
        ticks(DURATION_TICKS);
        assertEquals(1, energy(notch), "nach joinSeconds kommt niemand mehr dazu");
        assertEquals(5, energy(steve));
    }

    @Test
    void leavingDuringJoinPhaseAndReturningIsAllowed() {
        place(steve);
        ticks(100);
        steve.position = FAR_AWAY;
        ticks(100);
        steve.position = new Vec3(0, 64, 0);
        ticks(DURATION_TICKS);
        assertEquals(5, energy(steve));
    }

    @Test
    void leavingAfterJoinPhaseEliminates() {
        place(steve);
        ticks(JOIN_TICKS + 40);
        steve.position = FAR_AWAY;
        ticks(40);
        assertTrue(got(steve, "Kreis verlassen"), steve.messages.toString());
        steve.position = new Vec3(0, 64, 0);
        ticks(DURATION_TICKS);
        assertFalse(core.totem().isActive());
        assertEquals(2, energy(steve), "wer den Kreis verlassen hat, bekommt nichts – auch nach Rückkehr");
    }

    @Test
    void deathEliminates() {
        place(steve);
        ticks(100);
        core.onPlayerDeath(steve, null); // kostet 1 Energie
        assertEquals(1, energy(steve));
        assertTrue(got(steve, "gestorben"), steve.messages.toString());
        ticks(DURATION_TICKS);
        assertEquals(1, energy(steve), "Tote scheiden aus, auch wenn sie in der Beitrittszeit zurückkommen");
    }

    @Test
    void logoutEliminates() {
        place(steve);
        ticks(100);
        core.onPlayerQuit(steve.uuid());
        platform.quit(steve);
        ticks(100);
        FakePlayer back = platform.join("Steve");
        core.onPlayerJoin(back);
        ticks(DURATION_TICKS);
        assertEquals(2, energy(back), "nach dem Ausloggen zählt die Teilnahme nicht mehr");
    }

    @Test
    void hudShowsRemainingTimeForParticipants() {
        place(steve);
        ticks(40);
        assertTrue(steve.lastActionBar().contains("§d✚ 59s"), "Restzeit (aufgerundet): " + steve.lastActionBar());
        assertFalse(notch.lastActionBar().contains("✚"), notch.lastActionBar());
    }

    // ------------------------------------------------------------------ Zerstören

    @Test
    void destroyingTheTotemFailsForEveryone() {
        place(steve);
        ticks(JOIN_TICKS + 20);
        assertTrue(core.totem().onMiningProgress(notch, TestPlatform.WORLD, POS, 0));
        assertTrue(got(steve, "angegriffen"), steve.messages.toString());
        assertTrue(core.totem().onMiningProgress(notch, TestPlatform.WORLD, POS, 5));
        assertFalse(core.totem().onMiningProgress(notch, TestPlatform.WORLD, new BlockPos(0, 0, 0), 5),
                "anderer Block ist nicht das Totem");
        ticks(10);
        assertTrue(steve.lastActionBar().contains("§c⚠ Totem"), steve.lastActionBar());

        core.totem().onDestroyed(notch, TestPlatform.WORLD, POS);
        assertFalse(core.totem().isActive());
        assertTrue(platform.totems.isEmpty());
        assertTrue(got(alex, "Notch") && got(alex, "zerstört"), alex.messages.toString());

        ticks(DURATION_TICKS);
        assertEquals(2, energy(steve), "niemand bekommt Energie");
        assertEquals(7, energy(alex));
        assertTrue(core.ledger().recent(null, 50).stream().noneMatch(e -> EnergyLedger.TOTEM.equals(e.reason())));
    }

    @Test
    void cancelledMiningKeepsTheTotemRunning() {
        place(steve);
        ticks(20);
        core.totem().onMiningProgress(notch, TestPlatform.WORLD, POS, 3);
        assertTrue(core.totem().onMiningProgress(notch, TestPlatform.WORLD, POS, -1));
        ticks(DURATION_TICKS);
        assertEquals(5, energy(steve));
        assertEquals(10 * 20, core.totem().breakTicks(), "breakSeconds = 10, unabhängig vom Werkzeug");
    }

    @Test
    void vanishedBlockFailsTheRitual() {
        place(steve);
        ticks(100);
        platform.totems.clear(); // z. B. per /setblock entfernt
        ticks(20);
        assertFalse(core.totem().isActive());
        assertTrue(got(steve, "verschwunden"), steve.messages.toString());
        ticks(DURATION_TICKS);
        assertEquals(2, energy(steve));
    }

    @Test
    void adminCancelGivesNothing() {
        place(steve);
        ticks(100);
        assertTrue(core.totem().cancel("Admin"));
        assertFalse(core.totem().isActive());
        assertFalse(core.totem().cancel("Admin"));
        ticks(DURATION_TICKS);
        assertEquals(2, energy(steve));
    }

    // ------------------------------------------------------------------ Persistenz

    @Test
    void runningTotemSurvivesRestart() {
        place(steve);
        ticks(JOIN_TICKS + 100);
        Path file = platform.dataDirectory().resolve("totem.json");
        core.shutdown();
        assertTrue(Files.isRegularFile(file), "totem.json gespeichert");

        core = startCore();
        assertTrue(core.totem().isActive(), "Totem läuft nach dem Neustart weiter");
        for (FakePlayer player : new FakePlayer[]{steve, alex, notch}) {
            core.onPlayerJoin(player);
        }
        ticks(DURATION_TICKS - JOIN_TICKS - 100 + 20);
        assertFalse(core.totem().isActive());
        assertFalse(Files.exists(file), "totem.json nach dem Ende gelöscht");
        assertEquals(5, energy(steve), "Teilnahme übersteht den Neustart");
        assertEquals(7, energy(alex));
        assertEquals(1, energy(notch));
    }

    @Test
    void participantsHaveGracePeriodToReconnectAfterRestart() {
        core.config().get().totem.durationMinutes = 3;
        place(steve);
        alex.position = FAR_AWAY; // Alex nimmt nicht teil
        ticks(JOIN_TICKS + 100);
        core.shutdown();

        core = startCore();
        // Nach dem Neustart ist Steve noch nicht wieder da
        platform.quit(steve);
        ticks(40 * 20);
        assertTrue(core.totem().isActive());
        FakePlayer back = platform.join("Steve");
        core.onPlayerJoin(back);
        ticks(3 * 60 * 20);
        assertFalse(core.totem().isActive());
        assertEquals(5, energy(back), "innerhalb der Gnadenfrist zurück → Teilnahme bleibt");
    }

    @Test
    void participantsOfflineLongerThanGracePeriodAreEliminated() {
        core.config().get().totem.durationMinutes = 3;
        place(steve);
        ticks(JOIN_TICKS + 100);
        core.shutdown();

        core = startCore();
        platform.quit(steve);
        ticks(TotemService.RECONNECT_GRACE_TICKS + 40);
        FakePlayer back = platform.join("Steve");
        core.onPlayerJoin(back);
        ticks(3 * 60 * 20);
        assertFalse(core.totem().isActive());
        assertEquals(2, energy(back), "Gnadenfrist abgelaufen → ausgeschieden");
        assertEquals(7, energy(alex), "Alex war die ganze Zeit da und hatte schon genug");
    }
}
