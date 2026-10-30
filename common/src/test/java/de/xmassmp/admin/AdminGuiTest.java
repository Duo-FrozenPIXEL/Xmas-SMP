package de.xmassmp.admin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.xmassmp.MutableClock;
import de.xmassmp.TestPlatform;
import de.xmassmp.TestPlatform.FakePlayer;
import de.xmassmp.XmasCore;
import de.xmassmp.event.EventDefinition;
import de.xmassmp.gui.GuiButton;
import de.xmassmp.gui.GuiClick;
import de.xmassmp.gui.GuiHost;
import de.xmassmp.gui.GuiView;
import de.xmassmp.item.ItemSpec;
import de.xmassmp.player.PlayerData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Admin-GUI über den kompletten Kern: Navigation, Config-Änderungen, Spieler, Items und Events. */
class AdminGuiTest {

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-12-20T12:00:00Z"));
    private TestPlatform platform;
    private XmasCore core;
    private FakePlayer steve;
    private RecordingGuiHost host;
    private AdminGui gui;

    /** Merkt sich die zuletzt geöffnete GUI und wie oft geöffnet bzw. geschlossen wurde. */
    private static final class RecordingGuiHost implements GuiHost {
        GuiView last;
        UUID lastViewer;
        int opened;
        final List<UUID> closed = new ArrayList<>();

        @Override
        public boolean open(UUID player, GuiView view) {
            last = view;
            lastViewer = player;
            opened++;
            return true;
        }

        @Override
        public void close(UUID player) {
            closed.add(player);
        }
    }

    @BeforeEach
    void setUp() {
        platform = new TestPlatform(dir);
        core = new XmasCore(platform, clock);
        core.start();
        steve = platform.join("Steve");
        core.players().get(steve.uuid()).setCrystalId("fire");
        core.onPlayerJoin(steve);
        steve.messages.clear();
        host = new RecordingGuiHost();
        gui = new AdminGui(core.admin(), core.config(), core.crystals(), core.events(), core.players(),
                platform::onlinePlayers, host);
    }

    // ------------------------------------------------------------------ Hilfen

    private GuiView view() {
        assertTrue(host.last != null, "keine GUI geöffnet");
        return host.last;
    }

    private GuiButton button(int slot) {
        GuiButton button = view().button(slot);
        assertTrue(button != null, "Feld " + slot + " ist leer in " + view().title());
        return button;
    }

    /** Klickt ein Feld der aktuell offenen Seite und liefert die danach geöffnete Seite. */
    private GuiView click(int slot, GuiClick click) {
        button(slot).click(click);
        return view();
    }

    private GuiView click(int slot) {
        return click(slot, GuiClick.LEFT);
    }

    /** Erstes Feld, dessen Name {@code text} enthält. */
    private int slotNamed(String text) {
        for (Map.Entry<Integer, GuiButton> entry : view().buttons().entrySet()) {
            if (entry.getValue().name().contains(text)) {
                return entry.getKey();
            }
        }
        throw new AssertionError("Kein Feld mit \"" + text + "\" in " + view().title() + ": " + names());
    }

    private List<String> names() {
        return view().buttons().values().stream().map(GuiButton::name).toList();
    }

    private void openPage(int mainSlot) {
        assertTrue(gui.open(steve));
        click(mainSlot);
    }

    private List<JsonObject> audit() throws IOException {
        if (!Files.exists(core.audit().file())) {
            return List.of();
        }
        return Files.readAllLines(core.audit().file()).stream()
                .map(line -> JsonParser.parseString(line).getAsJsonObject()).toList();
    }

    private JsonObject lastAudit() throws IOException {
        List<JsonObject> entries = audit();
        assertFalse(entries.isEmpty(), "Audit-Log ist leer");
        return entries.get(entries.size() - 1);
    }

    // ------------------------------------------------------------------ Hauptmenü und Navigation

    @Test
    void openShowsMainMenu() {
        assertTrue(gui.open(steve), "Host unterstützt GUIs");
        assertEquals(steve.uuid(), host.lastViewer);
        GuiView main = view();
        assertEquals("§6Xmas-SMP §8– Admin", main.title());
        assertEquals(3, main.rows());
        assertEquals("§eAllgemein", button(10).name());
        assertEquals("§dKristalle", button(11).name());
        assertEquals("§6Energie", button(12).name());
        assertEquals("§bSpieler", button(13).name());
        assertEquals("§aEvents", button(14).name());
        assertEquals("§fItems", button(15).name());
        assertNull(button(16).onClick(), "Trader/Upgrader ist nur eine Anzeige");
        assertEquals("§cSchließen", button(26).name());
    }

    @Test
    void openReportsUnsupportedPlatform() {
        AdminGui unsupported = new AdminGui(core.admin(), core.config(), core.crystals(), core.events(),
                core.players(), platform::onlinePlayers, GuiHost.UNSUPPORTED);
        assertFalse(unsupported.open(steve));
    }

    @Test
    void navigationOpensSubpagesAndBack() {
        gui.open(steve);
        String[][] pages = {
                {"10", "§8Admin › §eAllgemein", "18"},
                {"11", "§8Admin › §dKristalle", "36"},
                {"12", "§8Admin › §6Energie", "18"},
                {"13", "§8Admin › §bSpieler", "45"},
                {"14", "§8Admin › §aEvents", "45"},
                {"15", "§8Admin › §fItems", "18"},
        };
        for (String[] page : pages) {
            assertEquals("§6Xmas-SMP §8– Admin", view().title());
            GuiView sub = click(Integer.parseInt(page[0]));
            assertEquals(page[1], sub.title());
            assertEquals("§7Zurück", button(Integer.parseInt(page[2])).name(), page[1]);
            click(Integer.parseInt(page[2]));
        }
        assertEquals("§6Xmas-SMP §8– Admin", view().title());
    }

    @Test
    void labelClickDoesNothingAndCloseCloses() {
        gui.open(steve);
        int before = host.opened;
        click(16);
        assertEquals(before, host.opened, "Anzeige-Feld öffnet keine neue Seite");
        click(26);
        assertEquals(List.of(steve.uuid()), host.closed);
    }

    // ------------------------------------------------------------------ Allgemein

    @Test
    void togglePluginWritesConfigKeepsCommentsAndAudits() throws IOException {
        openPage(10);
        assertEquals("§aPlugin: an", button(10).name());
        assertEquals("minecraft:lime_dye", button(10).icon());

        click(10);
        assertFalse(core.config().get().general.enabled, "Plugin ist aus");
        assertEquals("§8Admin › §eAllgemein", view().title(), "Seite wird neu aufgebaut");
        assertEquals("§cPlugin: aus", button(10).name());
        assertEquals("minecraft:gray_dye", button(10).icon());

        String text = Files.readString(dir.resolve("config.jsonc"));
        assertTrue(text.contains("// Hauptschalter für das ganze Plugin."), "Kommentar bleibt erhalten");
        assertTrue(text.contains("// Alle Kristall-Fähigkeiten global an/aus"), "andere Kommentare bleiben");
        assertTrue(text.replace(" ", "").contains("\"enabled\":false"), text);

        JsonObject entry = lastAudit();
        assertEquals("Steve", entry.get("actor").getAsString());
        assertEquals("config", entry.get("action").getAsString());
        assertEquals("general.enabled", entry.get("target").getAsString());
        assertEquals("false", entry.get("details").getAsString());
        assertTrue(steve.messages.isEmpty(), "Erfolg ohne Chat-Meldung: " + steve.messages);

        // Zurückschalten über die neu aufgebaute Seite.
        click(10);
        assertTrue(core.config().get().general.enabled);
        assertEquals("true", lastAudit().get("details").getAsString());
    }

    @Test
    void otherTogglesWork() {
        openPage(10);
        click(11);
        assertFalse(core.config().get().general.abilitiesEnabled);
        click(12);
        assertFalse(core.config().get().energy.pvpEnabled);
        click(16);
        assertFalse(core.config().get().hud.enabled);
        assertTrue(core.config().get().general.enabled, "Hauptschalter unverändert");
    }

    @Test
    void reloadButtonReloadsAndReports() throws IOException {
        openPage(10);
        Path config = dir.resolve("config.jsonc");
        Files.writeString(config, Files.readString(config).replace("\"pvpEnabled\": true", "\"pvpEnabled\": false"));
        click(22);
        assertFalse(core.config().get().energy.pvpEnabled, "Änderung in der Datei übernommen");
        assertTrue(steve.messages.get(steve.messages.size() - 1).contains("Neu geladen"), steve.messages.toString());
        assertEquals("reload", lastAudit().get("action").getAsString());
        assertEquals("§8Admin › §eAllgemein", view().title());
    }

    // ------------------------------------------------------------------ Energie

    @Test
    void energyValuesStepAndStayNonNegative() throws IOException {
        openPage(12);
        assertEquals("§eStart: §f5", button(10).name());

        click(10, GuiClick.LEFT);
        assertEquals(6, core.config().get().energy.start);
        assertEquals("§eStart: §f6", button(10).name());
        assertEquals("energy.start", lastAudit().get("target").getAsString());
        assertEquals("6", lastAudit().get("details").getAsString());

        click(10, GuiClick.SHIFT_RIGHT);
        assertEquals(1, core.config().get().energy.start, "Shift-Rechts zieht 5 ab");

        click(10, GuiClick.SHIFT_RIGHT);
        assertEquals(0, core.config().get().energy.start, "nicht unter 0");
        assertEquals("§eStart: §f0", button(10).name());

        click(10, GuiClick.RIGHT);
        assertEquals(0, core.config().get().energy.start, "bleibt bei 0");

        click(10, GuiClick.SHIFT_LEFT);
        assertEquals(5, core.config().get().energy.start, "Shift-Links addiert 5");

        assertTrue(Files.readString(dir.resolve("config.jsonc")).contains("// Energie neuer Spieler"),
                "Kommentar hinter dem Wert bleibt erhalten");
    }

    @Test
    void energyKillGainAndCooldownChange() {
        openPage(12);
        click(14, GuiClick.LEFT);
        assertEquals(2, core.config().get().energy.killGain);
        click(16, GuiClick.SHIFT_RIGHT);
        assertEquals(25, core.config().get().energy.sameVictimCooldownMinutes);
    }

    // ------------------------------------------------------------------ Kristalle

    @Test
    void crystalMultiplierStepsByTenth() throws IOException {
        openPage(11);
        int column = -1;
        for (int c = 0; c < GuiView.COLUMNS; c++) {
            GuiButton label = view().button(c);
            if (label != null && label.lore().contains("§8ID: fire")) {
                column = c;
            }
        }
        assertTrue(column >= 0, "Feuer-Spalte gefunden: " + names());
        int cooldown = 2 * GuiView.COLUMNS + column;
        int damage = 3 * GuiView.COLUMNS + column;
        assertEquals("§eCooldown × §f1.0", button(cooldown).name());

        click(cooldown, GuiClick.LEFT);
        assertEquals(1.1, core.config().get().crystal("fire").cooldownMultiplier, 1e-9);
        assertEquals("§eCooldown × §f1.1", button(cooldown).name());
        assertEquals("crystals.fire.cooldownMultiplier", lastAudit().get("target").getAsString());
        assertEquals(1.0, core.config().get().crystal("earth").cooldownMultiplier, 1e-9, "andere unverändert");

        click(damage, GuiClick.SHIFT_RIGHT);
        assertEquals(0.5, core.config().get().crystal("fire").damageMultiplier, 1e-9);
        for (int i = 0; i < 3; i++) {
            click(damage, GuiClick.SHIFT_RIGHT);
        }
        assertEquals(0.0, core.config().get().crystal("fire").damageMultiplier, 1e-9, "nicht unter 0");

        int toggle = GuiView.COLUMNS + column;
        assertTrue(button(toggle).name().contains("an"), button(toggle).name());
        click(toggle);
        assertFalse(core.config().get().crystal("fire").enabled);
        assertTrue(Files.readString(dir.resolve("config.jsonc")).contains("// Einstellungen je Kristall."));
    }

    // ------------------------------------------------------------------ Spieler

    @Test
    void playerListShowsOnlinePlayersSorted() {
        FakePlayer alex = platform.join("alex");
        core.onPlayerJoin(alex);
        openPage(13);
        assertEquals("§balex", button(0).name(), "Groß-/Kleinschreibung egal");
        assertEquals("§bSteve", button(1).name());
        assertTrue(button(1).lore().contains("§7Kristall: §fFeuer"), button(1).lore().toString());
    }

    @Test
    void emptyPlayerListShowsHint() {
        platform.quit(steve);
        assertTrue(gui.open(steve));
        click(13);
        assertEquals("§7Keine Spieler online", button(22).name());
    }

    @Test
    void playerPageChangesTierEnergyAndCooldowns() throws IOException {
        openPage(13);
        click(0);
        assertEquals("§8Admin › Spieler › §bSteve", view().title());
        PlayerData data = core.players().get(steve.uuid());

        // Tier wechselt 1 → 2 → 1.
        assertEquals(1, data.tier());
        click(12);
        assertEquals(2, data.tier());
        assertEquals("§eTier: §f2", button(12).name());
        assertEquals("set_tier", lastAudit().get("action").getAsString());
        assertEquals("1 -> 2", lastAudit().get("details").getAsString());
        click(12);
        assertEquals(1, data.tier());

        // Energie bleibt in min..max.
        assertEquals(5, data.energy());
        click(14, GuiClick.LEFT);
        assertEquals(6, data.energy());
        click(14, GuiClick.SHIFT_LEFT);
        assertEquals(10, data.energy(), "auf max begrenzt");
        click(14, GuiClick.LEFT);
        assertEquals(10, data.energy());
        for (int i = 0; i < 3; i++) {
            click(14, GuiClick.SHIFT_RIGHT);
        }
        assertEquals(0, data.energy(), "auf min begrenzt");
        click(14, GuiClick.RIGHT);
        assertEquals(0, data.energy());
        assertTrue(steve.messages.stream().noneMatch(m -> m.contains("Energie muss")), steve.messages.toString());

        // Cooldowns zurücksetzen.
        data.setCooldownUntil("fire_ball", clock.millis() + 60_000);
        assertFalse(data.cooldowns().isEmpty());
        click(16);
        assertTrue(data.cooldowns().isEmpty());
        assertEquals("reset_cooldowns", lastAudit().get("action").getAsString());

        // Zurück zur Spielerliste.
        click(18);
        assertEquals("§8Admin › §bSpieler", view().title());
    }

    @Test
    void playerPageCyclesCrystalAndSyncsItem() {
        openPage(13);
        click(0);
        List<String> ids = core.crystals().ids();
        PlayerData data = core.players().get(steve.uuid());
        int index = ids.indexOf("fire");
        click(10, GuiClick.LEFT);
        assertEquals(ids.get((index + 1) % ids.size()), data.crystalId());
        assertEquals(data.crystalId(), steve.crystals().get(0).data().get("crystal"), "Item wird angeglichen");
        click(10, GuiClick.RIGHT);
        assertEquals("fire", data.crystalId());
    }

    // ------------------------------------------------------------------ Items

    @Test
    void itemsPageGivesItemsToViewer() throws IOException {
        openPage(15);
        int before = steve.inventory.size();
        assertEquals("§fWiederbelebungs-Foliant", button(0).name());
        click(0);
        assertEquals(before + 1, steve.inventory.size());
        ItemSpec item = steve.inventory.get(steve.inventory.size() - 1);
        assertEquals("minecraft:enchanted_book", item.baseItem());
        assertEquals(1, item.customModelData());
        assertTrue(steve.messages.get(steve.messages.size() - 1).contains("erhalten"), steve.messages.toString());
        JsonObject entry = lastAudit();
        assertEquals("give", entry.get("action").getAsString());
        assertEquals("item=revival_tome", entry.get("details").getAsString());

        String firstCrystal = new ArrayList<>(core.crystals().all()).get(0).id();
        click(9, GuiClick.SHIFT_LEFT);
        assertEquals(before + 2, steve.inventory.size());
        ItemSpec crystal = steve.inventory.get(steve.inventory.size() - 1);
        assertEquals(firstCrystal, crystal.data().get("crystal"));
        assertEquals("2", crystal.data().get("tier"), "Shift-Klick: Tier 2");
        assertEquals("pristine", crystal.data().get("stage"));
        assertEquals("§8Admin › §fItems", view().title(), "Items-Seite bleibt offen");
    }

    // ------------------------------------------------------------------ Events

    @Test
    void emptyEventsFileShowsHint() throws IOException {
        Files.writeString(dir.resolve("events.jsonc"), "{ \"events\": [] }");
        core.reload();
        openPage(14);
        assertEquals("§7Keine Events in events.jsonc", button(22).name());
        assertEquals("§7Testmodus aus", button(49).name());
    }

    @Test
    void templateEventsCanBeEnabledAndStarted() throws IOException {
        openPage(14);
        List<EventDefinition> defs = new ArrayList<>(core.events().scheduler().definitions());
        assertEquals(4, defs.size(), "Vorlage hat 4 Events");
        assertTrue(defs.stream().noneMatch(EventDefinition::enabled), "alle deaktiviert");
        String id = defs.get(0).id();
        assertEquals("minecraft:gray_concrete", button(0).icon());
        assertTrue(button(0).name().contains("(" + id + ")"), button(0).name());

        // Rechtsklick: automatischen Start einschalten.
        Path file = dir.resolve("events.jsonc");
        String before = Files.readString(file);
        click(0, GuiClick.RIGHT);
        assertTrue(core.events().scheduler().definition(id).orElseThrow().enabled(), "Event aktiviert");
        assertEquals(3, core.events().scheduler().definitions().stream().filter(d -> !d.enabled()).count());
        String after = Files.readString(file);
        assertFalse(before.equals(after), "events.jsonc wurde geändert");
        assertTrue(after.contains("// Die Weihnachtswoche unten ist ein PLATZHALTER"), "Kopfkommentar bleibt");
        assertTrue(after.contains("// 8 Tage: 24.12. 00:00 bis 01.01. 00:00"), "Zeilenkommentar bleibt");
        assertEquals("minecraft:clock", button(0).icon());
        assertEquals("event_enabled", lastAudit().get("action").getAsString());
        assertEquals(id, lastAudit().get("target").getAsString());

        // Linksklick: jetzt starten.
        click(0, GuiClick.LEFT);
        assertTrue(core.events().scheduler().running(id).isPresent(), "Event läuft");
        assertEquals("minecraft:lime_concrete", button(0).icon());
        assertTrue(button(0).glow());
        assertTrue(button(0).lore().get(0).startsWith("§aläuft"), button(0).lore().toString());
        assertEquals("event_start", lastAudit().get("action").getAsString());

        // Linksklick auf das laufende Event: stoppen.
        click(0, GuiClick.LEFT);
        assertFalse(core.events().scheduler().running(id).isPresent(), "Event gestoppt");
        assertEquals("event_stop", lastAudit().get("action").getAsString());

        // Rechtsklick: wieder ausschalten.
        click(0, GuiClick.RIGHT);
        assertFalse(core.events().scheduler().definition(id).orElseThrow().enabled());
        assertTrue(steve.messages.isEmpty(), "keine Fehlermeldungen: " + steve.messages);
    }

    @Test
    void simulationButtonEndsTestMode() {
        assertTrue(core.admin().eventSimulate("Konsole", "2026-12-24", "17:55", true).success());
        openPage(14);
        assertTrue(button(49).name().startsWith("§dTestmodus: 24.12. 17:55"), button(49).name());
        click(49);
        assertFalse(core.events().isSimulating());
        assertEquals("§7Testmodus aus", button(49).name());
    }
}
