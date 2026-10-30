package de.xmassmp.gameplay;

import de.xmassmp.MutableClock;
import de.xmassmp.TestPlatform;
import de.xmassmp.TestPlatform.FakePlayer;
import de.xmassmp.XmasCore;
import de.xmassmp.crystal.Crystals;
import de.xmassmp.effect.EffectSink;
import de.xmassmp.event.EventHost;
import de.xmassmp.gui.GuiButton;
import de.xmassmp.gui.GuiClick;
import de.xmassmp.gui.GuiHost;
import de.xmassmp.gui.GuiView;
import de.xmassmp.item.CustomItem;
import de.xmassmp.item.ItemCatalog;
import de.xmassmp.platform.Platform;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.platform.TotemHost;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.player.PlayerData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Upgrader (Tier 1 → 2) und Gem Trader (Kristall frei wählen über die GUI). */
class UpgraderAndTraderTest {

    /** Merkt sich die zuletzt geöffnete GUI je Spieler, damit Tests Buttons anklicken können. */
    private static final class RecordingGui implements GuiHost {
        final Map<UUID, GuiView> open = new HashMap<>();
        int closes;

        @Override
        public boolean open(UUID player, GuiView view) {
            open.put(player, view);
            return true;
        }

        @Override
        public void close(UUID player) {
            open.remove(player);
            closes++;
        }
    }

    /** {@link TestPlatform} mit GUI-Unterstützung (im Original ist {@code guiHost()} UNSUPPORTED). */
    private record GuiPlatform(TestPlatform base, GuiHost gui) implements Platform {
        @Override
        public Path configDirectory() {
            return base.configDirectory();
        }

        @Override
        public Path dataDirectory() {
            return base.dataDirectory();
        }

        @Override
        public XmasLogger logger() {
            return base.logger();
        }

        @Override
        public EffectSink effectSink() {
            return base.effectSink();
        }

        @Override
        public EventHost eventHost() {
            return base.eventHost();
        }

        @Override
        public List<PlayerHandle> onlinePlayers() {
            return base.onlinePlayers();
        }

        @Override
        public Optional<PlayerHandle> player(UUID uuid) {
            return base.player(uuid);
        }

        @Override
        public GuiHost guiHost() {
            return gui;
        }

        @Override
        public TotemHost totemHost() {
            return base.totemHost();
        }

        @Override
        public void refreshCrafting() {
            base.refreshCrafting();
        }
    }

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-12-20T12:00:00Z"));
    private final RecordingGui gui = new RecordingGui();
    private TestPlatform platform;
    private XmasCore core;
    private FakePlayer steve;

    @BeforeEach
    void setUp() {
        platform = new TestPlatform(dir);
        core = new XmasCore(new GuiPlatform(platform, gui), clock);
        core.start();
        steve = platform.join("Steve");
        core.players().get(steve.uuid()).setCrystalId("fire");
        core.onPlayerJoin(steve);
    }

    private PlayerData data() {
        return core.players().get(steve.uuid());
    }

    private boolean use() {
        return core.onUseItem(steve, steve.mainHandData());
    }

    /** Button der offenen GUI, dessen Name {@code text} enthält. */
    private GuiButton button(String text) {
        GuiView view = gui.open.get(steve.uuid());
        assertNotNull(view, "keine GUI offen");
        return view.buttons().values().stream().filter(b -> b.name().contains(text)).findFirst()
                .orElseThrow(() -> new AssertionError("Button '" + text + "' fehlt: " + view.buttons().values()));
    }

    // ------------------------------------------------------------------ Upgrader

    @Test
    void upgraderRaisesTierOneToTwoAndIsConsumed() {
        assertEquals(1, data().tier());
        assertFalse(core.isTier2(steve.uuid()));
        int tabRefreshes = steve.tabRefreshes;

        steve.hold(ItemCatalog.custom(CustomItem.UPGRADER), 1);
        assertTrue(use());
        assertEquals(2, data().tier());
        assertNull(steve.mainHand, "Upgrader verbraucht");
        assertTrue(core.isTier2(steve.uuid()), "gelber Name in der Tab-Liste");
        assertTrue(steve.tabRefreshes > tabRefreshes, "Tab-Liste wird neu gesendet");
        assertTrue(steve.titles.stream().anyMatch(t -> t.contains("Tier 2")), steve.titles.toString());
        assertEquals(1, steve.crystals().size());
        assertEquals("2", steve.crystals().get(0).data().get("tier"), "Kristall-Item zeigt Tier 2");
    }

    @Test
    void upgraderAtTierTwoConsumesNothing() {
        data().setTier(2);
        steve.hold(ItemCatalog.custom(CustomItem.UPGRADER), 1);
        assertTrue(use());
        assertEquals(2, data().tier());
        assertEquals(1, steve.mainHandCount, "nichts verbraucht");
        assertTrue(steve.lastActionBar().contains("schon Tier 2"), steve.actionBar.toString());
    }

    @Test
    void upgraderWithoutCrystalOrDisabledConsumesNothing() {
        core.config().get().upgrader.enabled = false;
        steve.hold(ItemCatalog.custom(CustomItem.UPGRADER), 1);
        assertTrue(use());
        assertEquals(1, data().tier());
        assertEquals(1, steve.mainHandCount);

        core.config().get().upgrader.enabled = true;
        data().setCrystalId(null);
        assertTrue(use());
        assertEquals(1, steve.mainHandCount);
        assertFalse(core.isTier2(steve.uuid()));
    }

    @Test
    void isTier2RespectsConfigSwitch() {
        data().setTier(2);
        assertTrue(core.isTier2(steve.uuid()));
        core.config().get().controls.tier2TabColor = false;
        assertFalse(core.isTier2(steve.uuid()));
        assertFalse(core.isTier2(UUID.randomUUID()), "unbekannte Spieler sind nie Tier 2");
    }

    // ------------------------------------------------------------------ Trader

    @Test
    void traderSwapsCrystalKeepingTierAndEnergy() {
        data().setTier(2);
        data().setEnergy(7);
        data().setCooldownUntil("fire.flame_burst", clock.millis() + 60_000);
        steve.hold(ItemCatalog.custom(CustomItem.GEM_TRADER), 1);

        assertTrue(use());
        assertEquals(1, steve.mainHandCount, "erst die Auswahl verbraucht den Händler");
        button("Wind").click(GuiClick.LEFT);

        assertEquals("wind", data().crystalId());
        assertEquals(2, data().tier(), "Tier bleibt");
        assertEquals(7, data().energy(), "Energie bleibt");
        assertTrue(data().cooldowns().isEmpty(), "Cooldowns werden zurückgesetzt");
        assertNull(steve.mainHand, "Händler verbraucht");
        assertFalse(gui.open.containsKey(steve.uuid()), "GUI geschlossen");
        assertTrue(steve.messages.stream().anyMatch(m -> m.contains("getauscht")), steve.messages.toString());
        assertEquals(1, steve.crystals().size(), "genau ein Kristall-Item");
        assertEquals("wind", steve.crystals().get(0).data().get("crystal"), "Kristall-Item wurde ersetzt");
        assertEquals("2", steve.crystals().get(0).data().get("tier"));
    }

    @Test
    void traderWithoutItemInHandDoesNothing() {
        steve.hold(ItemCatalog.custom(CustomItem.GEM_TRADER), 1);
        use();
        steve.hold(null, 0); // Händler inzwischen weggelegt
        button("Wind").click(GuiClick.LEFT);
        assertEquals("fire", data().crystalId());
        assertTrue(steve.lastActionBar().contains("Halte den Kristall-Händler"), steve.actionBar.toString());

        // Auch ein anderes Custom-Item in der Hand zählt nicht
        steve.hold(ItemCatalog.custom(CustomItem.UPGRADER), 1);
        use(); // Upgrader wirkt normal (Tier 2)
        steve.hold(ItemCatalog.custom(CustomItem.GEM_TRADER), 1);
        use();
        steve.hold(ItemCatalog.custom(CustomItem.REPAIR_KIT), 1);
        button("Natur").click(GuiClick.LEFT);
        assertEquals("fire", data().crystalId());
        assertEquals(1, steve.mainHandCount, "Repair Kit bleibt");
    }

    @Test
    void ownCrystalIsOnlyALabel() {
        steve.hold(ItemCatalog.custom(CustomItem.GEM_TRADER), 1);
        use();
        GuiButton own = button("Feuer");
        assertNull(own.onClick(), "eigener Kristall ist nicht wählbar");
        assertTrue(own.lore().stream().anyMatch(l -> l.contains("aktueller Kristall")), own.lore().toString());
        own.click(GuiClick.LEFT);
        assertEquals("fire", data().crystalId());
        assertEquals(1, steve.mainHandCount);

        for (String other : List.of("Erde", "Wind", "Schatten", "Natur")) {
            assertNotNull(button(other).onClick(), other + " ist wählbar");
        }
    }

    @Test
    void cancelButtonConsumesNothing() {
        steve.hold(ItemCatalog.custom(CustomItem.GEM_TRADER), 1);
        use();
        button("Abbrechen").click(GuiClick.LEFT);
        assertFalse(gui.open.containsKey(steve.uuid()));
        assertEquals("fire", data().crystalId());
        assertEquals(1, steve.mainHandCount);
    }

    @Test
    void traderWithoutKeepTierAndEnergyResetsBoth() {
        core.config().get().trader.keepTier = false;
        core.config().get().trader.keepEnergy = false;
        data().setTier(2);
        data().setEnergy(9);
        steve.hold(ItemCatalog.custom(CustomItem.GEM_TRADER), 1);
        use();
        button("Schatten").click(GuiClick.LEFT);
        assertEquals("shadow", data().crystalId());
        assertEquals(1, data().tier());
        assertEquals(5, data().energy(), "energy.start");
    }

    @Test
    void viewAndChooseWorkWithoutGuiSupport() {
        // Direkt über die paketinternen Methoden, wie auf einer Plattform ohne GUI
        TraderService trader = new TraderService(core.crystals(), core.players(), core.config()::get,
                GuiHost.UNSUPPORTED, core.effects(), core.hud(), clock::millis, uuid -> { });
        GuiView view = trader.view(steve);
        assertTrue(view.buttons().values().stream().anyMatch(b -> b.name().contains("Feuer") && b.onClick() == null));

        steve.hold(ItemCatalog.custom(CustomItem.GEM_TRADER), 1);
        trader.choose(steve, Crystals.FIRE);
        assertEquals(1, steve.mainHandCount, "eigener Kristall: nichts passiert");
        trader.choose(steve, Crystals.EARTH);
        assertEquals("earth", data().crystalId());
        assertNull(steve.mainHand);
    }
}
