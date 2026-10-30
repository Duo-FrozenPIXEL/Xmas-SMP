package de.xmassmp.bottle;

import de.xmassmp.MutableClock;
import de.xmassmp.TestPlatform;
import de.xmassmp.TestPlatform.FakePlayer;
import de.xmassmp.XmasCore;
import de.xmassmp.energy.EnergyLedger;
import de.xmassmp.item.CustomItem;
import de.xmassmp.item.ItemCatalog;
import de.xmassmp.item.ItemSpec;
import de.xmassmp.util.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Energy Bottles: abfüllen (mit Abbrüchen und Sperren), trinken, Kopien/Fälschungen, zerlegen, Persistenz. */
class BottleServiceTest {

    private static final String GLASS = BottleService.GLASS_BOTTLE;
    /** Standard: {@code energyBottle.chargeSeconds} = 5. */
    private static final int CHARGE_TICKS = 100;

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-12-20T12:00:00Z"));
    private TestPlatform platform;
    private XmasCore core;
    private FakePlayer steve;
    private FakePlayer alex;

    @BeforeEach
    void setUp() {
        platform = new TestPlatform(dir);
        core = new XmasCore(platform, clock);
        core.start();
        steve = platform.join("Steve");
        alex = platform.join("Alex");
        core.players().get(steve.uuid()).setCrystalId("fire");
        core.players().get(alex.uuid()).setCrystalId("wind");
        core.onPlayerJoin(steve);
        core.onPlayerJoin(alex);
    }

    // ------------------------------------------------------------------ Hilfen

    private int energy(FakePlayer player) {
        return core.players().get(player.uuid()).energy();
    }

    private void setEnergy(FakePlayer player, int value) {
        core.players().get(player.uuid()).setEnergy(value);
    }

    /** Schleichen + Glasflasche in der Hand, erster Rechtsklick. */
    private boolean startFill(FakePlayer player) {
        player.sneaking = true;
        if (player.mainHand == null) {
            player.hold(ItemSpec.vanilla(GLASS), 1);
        }
        return core.onUseVanillaItem(player, GLASS);
    }

    /**
     * {@code n} Server-Ticks mit gehaltener Taste: Wie der Client wiederholt der Adapter die Nutzung etwa
     * alle 4 Ticks, solange eine Glasflasche in der Hand liegt.
     */
    private void holdFor(FakePlayer player, int n) {
        for (int i = 1; i <= n; i++) {
            core.tick();
            if (i % 4 == 0 && GLASS.equals(player.mainHandItem())) {
                core.onUseVanillaItem(player, GLASS);
            }
        }
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            core.tick();
        }
    }

    private List<ItemSpec> bottles(FakePlayer player) {
        return player.inventory.stream()
                .filter(stack -> CustomItem.ENERGY_BOTTLE.id().equals(stack.data().get(ItemSpec.DATA_TYPE)))
                .toList();
    }

    private static String idOf(ItemSpec bottle) {
        return bottle.data().get(ItemCatalog.DATA_BOTTLE);
    }

    /** Eine gültige, registrierte Flasche in die Hand legen. */
    private ItemSpec holdNewBottle(FakePlayer player) {
        ItemSpec bottle = core.bottles().create("admin", "Test");
        player.hold(bottle, 1);
        return bottle;
    }

    private boolean drink(FakePlayer player) {
        return core.onUseItem(player, player.mainHandData());
    }

    private List<EnergyLedger.Entry> ledger(FakePlayer player, String reason) {
        return core.ledger().recent(player.uuid(), 100).stream().filter(e -> reason.equals(e.reason())).toList();
    }

    // ------------------------------------------------------------------ Abfüllen

    @Test
    void fillingSucceedsAfterFullChargeWithRepeatedInput() {
        assertTrue(startFill(steve), "Vanilla-Verhalten (Wasser schöpfen) wird unterdrückt");
        assertTrue(core.bottles().isCharging(steve.uuid()));

        holdFor(steve, CHARGE_TICKS - 1);
        assertTrue(core.bottles().isCharging(steve.uuid()), "kurz vor Ende läuft das Abfüllen noch");
        assertEquals(5, energy(steve), "Energie wird erst am Ende abgezogen");
        assertTrue(bottles(steve).isEmpty());

        holdFor(steve, 1);
        assertFalse(core.bottles().isCharging(steve.uuid()));
        assertEquals(4, energy(steve), "Energie −1");
        assertNull(steve.mainHand, "Glasflasche ist verbraucht");
        assertEquals(1, bottles(steve).size(), "neue Energy Bottle im Inventar");
        String id = idOf(bottles(steve).get(0));
        assertNotNull(id, "Flasche trägt eine ID");
        assertEquals(BottleRegistry.State.ACTIVE, core.bottles().registry().state(id));
        assertEquals("fill", core.bottles().registry().entry(id).origin);
        assertEquals("Steve", core.bottles().registry().entry(id).creator);

        List<EnergyLedger.Entry> fills = ledger(steve, EnergyLedger.BOTTLE_FILL);
        assertEquals(1, fills.size(), "Ledger-Eintrag bottle_fill");
        assertEquals(5, fills.get(0).from());
        assertEquals(4, fills.get(0).to());
        assertEquals(id, fills.get(0).ref(), "Ledger verweist auf die Flaschen-ID");
    }

    @Test
    void fillingFromStackUsesOnlyOneGlassBottle() {
        steve.hold(ItemSpec.vanilla(GLASS), 3);
        startFill(steve);
        holdFor(steve, CHARGE_TICKS);
        assertEquals(2, steve.mainHandCount, "nur eine Glasflasche verbraucht");
        assertEquals(1, bottles(steve).size());
        assertEquals(4, energy(steve));
    }

    @Test
    void releasingTheKeyAbortsAfterMoreThanSixTicks() {
        startFill(steve);
        holdFor(steve, 40);
        // Keine Eingabe mehr: 6 Ticks gelten noch als gehalten, ab dem 7. ist die Taste losgelassen.
        ticks(BottleService.RELEASE_TICKS);
        assertTrue(core.bottles().isCharging(steve.uuid()), "6 Ticks ohne Paket sind noch erlaubt");
        ticks(1);
        assertFalse(core.bottles().isCharging(steve.uuid()), "Abbruch nach > 6 Ticks ohne Eingabe");

        ticks(CHARGE_TICKS);
        assertEquals(5, energy(steve));
        assertEquals(GLASS, steve.mainHandItem(), "Glasflasche bleibt");
        assertTrue(bottles(steve).isEmpty());
        assertTrue(core.bottles().registry().activeIds().isEmpty());
    }

    @Test
    void damageAbortsFilling() {
        startFill(steve);
        holdFor(steve, 40);
        core.onPlayerDamaged(steve, null);
        assertFalse(core.bottles().isCharging(steve.uuid()));
        assertTrue(steve.lastActionBar().contains("Schaden"), steve.actionBar.toString());

        holdFor(steve, CHARGE_TICKS + 8);
        // Die weiter gehaltene Taste startet beim nächsten Paket einen neuen Vorgang (Mob-Schaden setzt
        // keine Kampfsperre), der erst nach voller Ladezeit fertig wird.
        assertEquals(4, energy(steve));
        assertEquals(1, bottles(steve).size());
    }

    @Test
    void damageAbortRestartsTheFullChargeTime() {
        startFill(steve);
        holdFor(steve, 80);
        core.onPlayerDamaged(steve, null);
        holdFor(steve, 40);
        assertEquals(5, energy(steve), "der bisherige Fortschritt zählt nach dem Abbruch nicht mehr");
        assertTrue(bottles(steve).isEmpty());
    }

    @Test
    void movingAbortsFilling() {
        startFill(steve);
        holdFor(steve, 20);
        steve.position = new Vec3(0.5, 64, 0.5);
        holdFor(steve, 4);
        assertTrue(core.bottles().isCharging(steve.uuid()), "kleine Bewegungen sind erlaubt");

        steve.position = new Vec3(2, 64, 0);
        holdFor(steve, 1);
        assertFalse(core.bottles().isCharging(steve.uuid()));
        assertTrue(steve.lastActionBar().contains("bewegt"), steve.actionBar.toString());
        assertEquals(5, energy(steve));
    }

    @Test
    void switchingSlotAbortsFilling() {
        startFill(steve);
        holdFor(steve, 20);
        steve.selectedSlot = 4;
        holdFor(steve, 1);
        assertFalse(core.bottles().isCharging(steve.uuid()));
        assertTrue(steve.lastActionBar().contains("gewechselt"), steve.actionBar.toString());
        assertEquals(5, energy(steve));
        assertTrue(bottles(steve).isEmpty());
    }

    @Test
    void stopSneakingAbortsFilling() {
        startFill(steve);
        holdFor(steve, 20);
        steve.sneaking = false;
        holdFor(steve, 1);
        assertFalse(core.bottles().isCharging(steve.uuid()));
        assertEquals(5, energy(steve));
    }

    @Test
    void notSneakingIsNotHandled() {
        steve.hold(ItemSpec.vanilla(GLASS), 1);
        steve.sneaking = false;
        assertFalse(core.onUseVanillaItem(steve, GLASS), "ohne Schleichen normales Vanilla-Verhalten");
        assertFalse(core.bottles().isCharging(steve.uuid()));
    }

    @Test
    void tooLittleEnergyBlocksFilling() {
        setEnergy(steve, 1); // keepAtLeast = 1 → nach dem Abfüllen bliebe 0
        assertTrue(startFill(steve));
        assertFalse(core.bottles().isCharging(steve.uuid()));
        assertTrue(steve.lastActionBar().contains("mindestens 2 Energie"), steve.actionBar.toString());
        holdFor(steve, CHARGE_TICKS + 10);
        assertEquals(1, energy(steve));
        assertTrue(bottles(steve).isEmpty());

        setEnergy(steve, 2);
        startFill(steve);
        holdFor(steve, CHARGE_TICKS);
        assertEquals(1, energy(steve), "mit 2 Energie geht es genau noch");
        assertEquals(1, bottles(steve).size());
    }

    @Test
    void combatTagBlocksFillingForBothPlayers() {
        core.onPlayerDamaged(steve, alex);
        assertTrue(startFill(steve));
        assertFalse(core.bottles().isCharging(steve.uuid()));
        assertTrue(steve.lastActionBar().contains("Im Kampf"), steve.actionBar.toString());

        assertTrue(startFill(alex));
        assertFalse(core.bottles().isCharging(alex.uuid()), "auch der Angreifer ist im Kampf");

        clock.advance(Duration.ofSeconds(14));
        startFill(steve);
        assertFalse(core.bottles().isCharging(steve.uuid()), "combatTagSeconds = 15 noch nicht vorbei");

        clock.advance(Duration.ofSeconds(1));
        startFill(steve);
        assertTrue(core.bottles().isCharging(steve.uuid()), "nach 15 s wieder erlaubt");
    }

    @Test
    void creativePlayersCannotFill() {
        steve.creative = true;
        startFill(steve);
        assertFalse(core.bottles().isCharging(steve.uuid()));
        assertTrue(steve.lastActionBar().contains("Kreativ"), steve.actionBar.toString());
    }

    @Test
    void quitAndDeathCancelFilling() {
        startFill(steve);
        holdFor(steve, 20);
        core.onPlayerDeath(steve, null);
        assertFalse(core.bottles().isCharging(steve.uuid()));

        startFill(alex);
        holdFor(alex, 20);
        core.onPlayerQuit(alex.uuid());
        platform.quit(alex);
        assertFalse(core.bottles().isCharging(alex.uuid()));
        assertTrue(core.bottles().registry().activeIds().isEmpty());
    }

    // ------------------------------------------------------------------ Trinken

    @Test
    void drinkingGivesOneEnergyAndClosesTheBottle() {
        ItemSpec bottle = holdNewBottle(steve);
        assertTrue(drink(steve));
        assertEquals(6, energy(steve));
        assertNull(steve.mainHand, "Flasche ist weg");
        assertTrue(steve.inventory.stream().anyMatch(s -> GLASS.equals(s.baseItem()) && s.isVanilla()),
                "leere Glasflasche zurück");
        assertEquals(BottleRegistry.State.USED, core.bottles().registry().state(idOf(bottle)));
        assertEquals("drink", core.bottles().registry().entry(idOf(bottle)).closedReason);
        List<EnergyLedger.Entry> drinks = ledger(steve, EnergyLedger.BOTTLE_DRINK);
        assertEquals(1, drinks.size());
        assertEquals(idOf(bottle), drinks.get(0).ref());
    }

    @Test
    void drinkingWorksAtZeroEnergy() {
        setEnergy(steve, 0);
        holdNewBottle(steve);
        assertTrue(drink(steve));
        assertEquals(1, energy(steve), "auch ein zerbrochener Kristall nimmt Energie auf");
    }

    @Test
    void drinkingAtMaxEnergyKeepsTheBottle() {
        setEnergy(steve, 10);
        ItemSpec bottle = holdNewBottle(steve);
        assertTrue(drink(steve));
        assertEquals(10, energy(steve));
        assertEquals(bottle, steve.mainHand, "Flasche bleibt in der Hand");
        assertEquals(1, steve.mainHandCount);
        assertEquals(BottleRegistry.State.ACTIVE, core.bottles().registry().state(idOf(bottle)));
        assertTrue(steve.lastActionBar().contains("voll"), steve.actionBar.toString());
    }

    @Test
    void drinkCooldownBlocksSecondBottleWithinOneSecond() {
        holdNewBottle(steve);
        drink(steve);
        ItemSpec second = holdNewBottle(steve);
        drink(steve);
        assertEquals(6, energy(steve), "drinkCooldownSeconds = 1");
        assertEquals(second, steve.mainHand);

        clock.advance(Duration.ofSeconds(1));
        drink(steve);
        assertEquals(7, energy(steve));
        assertNull(steve.mainHand);
    }

    @Test
    void copyWithSameIdWorksOnlyOnceAndIsRemoved() {
        ItemSpec bottle = holdNewBottle(steve);
        drink(steve);
        assertEquals(6, energy(steve));

        clock.advance(Duration.ofSeconds(5));
        // Kopie (Dupe-Glitch, Creative-Mittelklick) mit derselben ID, auch bei einem anderen Spieler
        alex.hold(bottle, 1);
        assertTrue(drink(alex));
        assertEquals(5, energy(alex), "Kopie wirkt nicht");
        assertNull(alex.mainHand, "Kopie wurde entfernt");
        assertTrue(alex.messages.stream().anyMatch(m -> m.contains("ungültig")), alex.messages.toString());
        List<EnergyLedger.Entry> suspicious = ledger(alex, EnergyLedger.SUSPICIOUS);
        assertEquals(1, suspicious.size(), "Ledger vermerkt die Kopie");
        assertTrue(suspicious.get(0).ref().startsWith(idOf(bottle)), suspicious.get(0).ref());
        assertTrue(suspicious.get(0).ref().contains("Kopie"), suspicious.get(0).ref());
        assertEquals(suspicious.get(0).from(), suspicious.get(0).to(), "kein Energiefluss");

        steve.hold(bottle, 1);
        drink(steve);
        assertEquals(6, energy(steve), "auch beim Ersteller wirkt die Kopie nicht");
    }

    @Test
    void forgedBottleWithoutRegistryEntryHasNoEffect() {
        steve.hold(ItemCatalog.energyBottle(UUID.randomUUID()), 1);
        assertTrue(drink(steve));
        assertEquals(5, energy(steve));
        assertNull(steve.mainHand, "Fälschung wird entfernt");
        List<EnergyLedger.Entry> suspicious = ledger(steve, EnergyLedger.SUSPICIOUS);
        assertEquals(1, suspicious.size());
        assertTrue(suspicious.get(0).ref().contains("Fälschung"), suspicious.get(0).ref());
    }

    @Test
    void bottleWithoutIdHasNoEffect() {
        steve.hold(ItemCatalog.custom(CustomItem.ENERGY_BOTTLE), 1);
        assertTrue(drink(steve));
        assertEquals(5, energy(steve));
        assertNull(steve.mainHand);
        assertTrue(ledger(steve, EnergyLedger.SUSPICIOUS).get(0).ref().startsWith("ohne-id"));
    }

    // ------------------------------------------------------------------ Zerlegen

    @Test
    void craftingClosesTheBottleAndRefreshesCraftingGrids() {
        ItemSpec bottle = core.bottles().create("admin", "Test");
        assertTrue(core.bottles().canCraft(bottle.data()));
        int before = platform.craftingRefreshes;

        assertTrue(core.bottles().consumeForCraft(bottle.data(), "Steve"));
        assertEquals(before + 1, platform.craftingRefreshes, "Werkbänke werden neu berechnet");
        assertEquals(BottleRegistry.State.USED, core.bottles().registry().state(idOf(bottle)));
        assertEquals("craft", core.bottles().registry().entry(idOf(bottle)).closedReason);

        assertFalse(core.bottles().canCraft(bottle.data()), "Kopie im selben Feld zeigt kein Ergebnis mehr");
        assertFalse(core.bottles().consumeForCraft(bottle.data(), "Steve"), "zweites Zerlegen scheitert");

        steve.hold(bottle, 1);
        drink(steve);
        assertEquals(5, energy(steve), "zerlegte Flasche lässt sich nicht mehr trinken");
    }

    @Test
    void drinkingRefreshesCraftingSoCopiesCannotBeCrafted() {
        ItemSpec bottle = holdNewBottle(steve);
        int before = platform.craftingRefreshes;
        drink(steve);
        assertTrue(platform.craftingRefreshes > before, "nach dem Trinken werden Werkbänke neu berechnet");
        assertFalse(core.bottles().canCraft(bottle.data()), "Kopie einer getrunkenen Flasche ist nicht zerlegbar");
    }

    @Test
    void forgedBottleCannotBeCrafted() {
        assertFalse(core.bottles().canCraft(ItemCatalog.energyBottle(UUID.randomUUID()).data()));
        assertFalse(core.bottles().canCraft(Map.of(ItemSpec.DATA_TYPE, CustomItem.ENERGY_BOTTLE.id())));
    }

    // ------------------------------------------------------------------ Sieger mit voller Energie

    @Test
    void overflowBottleIsRegisteredAndDrinkable() {
        setEnergy(steve, 10);
        core.onPlayerDeath(alex, steve);
        assertEquals(1, alex.dropped.size());
        ItemSpec dropped = alex.dropped.get(0);
        assertEquals("overflow", core.bottles().registry().entry(idOf(dropped)).origin);

        // Jeder kann die Flasche aufheben, z. B. das Opfer selbst
        alex.hold(dropped, 1);
        drink(alex);
        assertEquals(5, energy(alex), "4 nach dem Tod, +1 aus der Flasche");
    }

    // ------------------------------------------------------------------ Persistenz

    @Test
    void registrySurvivesReloadFromFile() {
        ItemSpec active = core.bottles().create("admin", "Test");
        ItemSpec used = core.bottles().create("admin", "Test");
        core.bottles().registry().close(idOf(used), "Steve", "drink");

        BottleRegistry reloaded = new BottleRegistry(platform.dataDirectory(), clock, platform.logger());
        reloaded.load();
        assertEquals(BottleRegistry.State.ACTIVE, reloaded.state(idOf(active)));
        assertEquals(BottleRegistry.State.USED, reloaded.state(idOf(used)));
        assertEquals("drink", reloaded.entry(idOf(used)).closedReason);
        assertEquals(BottleRegistry.State.UNKNOWN, reloaded.state(UUID.randomUUID().toString()));

        // Auch ein neu gestarteter Kern kennt die Flaschen
        core.shutdown();
        XmasCore restarted = new XmasCore(platform, clock);
        restarted.start();
        assertEquals(BottleRegistry.State.ACTIVE, restarted.bottles().registry().state(idOf(active)));
        assertEquals(BottleRegistry.State.USED, restarted.bottles().registry().state(idOf(used)));
        restarted.players().get(steve.uuid()).setCrystalId("fire");
        steve.hold(used, 1);
        restarted.onUseItem(steve, steve.mainHandData());
        assertEquals(5, restarted.players().get(steve.uuid()).energy(), "verbrauchte Flasche bleibt nach Neustart wirkungslos");
        assertTrue(platform.errors.isEmpty(), platform.errors.toString());
    }
}
