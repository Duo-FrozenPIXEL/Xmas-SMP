package de.xmassmp.recipe;

import de.xmassmp.MutableClock;
import de.xmassmp.TestPlatform;
import de.xmassmp.XmasCore;
import de.xmassmp.config.RecipeSpec;
import de.xmassmp.item.CustomItem;
import de.xmassmp.item.ItemCatalog;
import de.xmassmp.item.ItemSpec;
import de.xmassmp.recipe.RecipeService.Cell;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Werkbank-Rezepte aus der Config: geformt (Position, Spiegelung), formlos für Fragments, Abschalten. */
class RecipeServiceTest {

    private static final String UPGRADER = CustomItem.UPGRADER.id();
    private static final String TRADER = CustomItem.GEM_TRADER.id();
    private static final String REPAIR_KIT = CustomItem.REPAIR_KIT.id();

    /** Zeichen im Test-Gitter → Item-ID; {@code .} = leeres Feld. */
    private static final Map<Character, String> LEGEND = Map.of(
            'F', "xmassmp:gem_fragment",
            'B', "minecraft:dragon_breath",
            'W', "minecraft:wither_skeleton_skull",
            'D', "minecraft:diamond",
            'K', "minecraft:diamond_block",
            'A', "minecraft:anvil",
            'T', "minecraft:netherite_upgrade_smithing_template",
            'N', "minecraft:netherite_ingot",
            'S', "minecraft:stone");

    @TempDir
    Path dir;

    private XmasCore core;

    @BeforeEach
    void setUp() {
        core = new XmasCore(new TestPlatform(dir), new MutableClock(Instant.parse("2026-12-20T12:00:00Z")));
        core.start();
    }

    // ------------------------------------------------------------------ Hilfen

    /** Gitter aus Zeilen gleicher Länge, z. B. {@code grid("FBF", "BWB", "FBF")}. */
    private static List<Cell> grid(String... rows) {
        List<Cell> cells = new ArrayList<>();
        for (String row : rows) {
            for (char c : row.toCharArray()) {
                cells.add(c == '.' ? Cell.EMPTY : new Cell(LEGEND.get(c), Map.of()));
            }
        }
        return cells;
    }

    private boolean matches(String id, String... rows) {
        return core.recipes().matches(id, rows[0].length(), rows.length, grid(rows));
    }

    /** Ersetzt {@code config.jsonc} durch {@code json} und lädt neu; liefert die Meldungen. */
    private List<String> writeConfig(String json) throws IOException {
        Files.writeString(dir.resolve("config.jsonc"), json);
        var result = core.reloadResult();
        assertTrue(result.success(), result.messages().toString());
        return result.messages();
    }

    private static List<Cell> withBottleAt(int size, int index, Map<String, String> data) {
        List<Cell> cells = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            cells.add(i == index ? new Cell(CustomItem.ENERGY_BOTTLE.itemId(), data) : Cell.EMPTY);
        }
        return cells;
    }

    // ------------------------------------------------------------------ Standardrezepte

    @Test
    void defaultShapedRecipesMatchInCraftingTable() {
        assertTrue(matches(UPGRADER, "FBF", "BWB", "FBF"));
        assertTrue(matches(TRADER, "DBD", "BFB", "DBD"));
        assertTrue(matches(REPAIR_KIT, "FAF", "KTK", "FAF"));
    }

    @Test
    void wrongIngredientDoesNotMatch() {
        assertFalse(matches(UPGRADER, "FBF", "BSB", "FBF"), "Stein statt Wither-Schädel");
        assertFalse(matches(UPGRADER, "FBF", "BWB", "FB."), "eine Zutat fehlt");
        assertFalse(matches(REPAIR_KIT, "FAF", "NTN", "FAF"), "Netherit statt Diamantblöcken");
        assertFalse(matches(TRADER, "FBF", "BWB", "FBF"), "Upgrader-Muster ist kein Trader");
    }

    @Test
    void threeByThreeRecipesDoNotFitTwoByTwoGrid() {
        assertFalse(matches(UPGRADER, "FB", "BW"));
        assertFalse(matches(UPGRADER, "..", ".."));
    }

    @Test
    void unknownRecipeAndWrongCellCountDoNotMatch() {
        assertFalse(core.recipes().matches("does_not_exist", 3, 3, grid("FBF", "BWB", "FBF")));
        assertFalse(core.recipes().matches(UPGRADER, 3, 3, grid("FBF", "BWB")), "Zellenzahl passt nicht zu 3×3");
    }

    @Test
    void resultsAndIds() {
        assertEquals(List.of(UPGRADER, TRADER, REPAIR_KIT, RecipeSpec.GEM_FRAGMENTS), RecipeService.ids());
        RecipeService.Result upgrader = core.recipes().result(UPGRADER).orElseThrow();
        assertEquals(UPGRADER, upgrader.item().data().get(ItemSpec.DATA_TYPE));
        assertEquals(1, upgrader.count());
        RecipeService.Result fragments = core.recipes().result(RecipeSpec.GEM_FRAGMENTS).orElseThrow();
        assertEquals(CustomItem.GEM_FRAGMENT.id(), fragments.item().data().get(ItemSpec.DATA_TYPE));
        assertEquals(9, fragments.count());
        assertTrue(core.recipes().result("does_not_exist").isEmpty());
    }

    // ------------------------------------------------------------------ Position und Spiegelung

    @Test
    void smallShapedRecipeMatchesAtEveryPositionAndMirrored() throws IOException {
        writeConfig("""
                { "recipes": { "upgrader": {
                    "pattern": ["FB", "W "],
                    "key": { "F": "xmassmp:gem_fragment", "B": "minecraft:dragon_breath",
                             "W": "minecraft:wither_skeleton_skull" } } } }
                """);
        // an jeder Stelle des 3×3-Gitters
        assertTrue(matches(UPGRADER, "FB.", "W..", "..."));
        assertTrue(matches(UPGRADER, ".FB", ".W.", "..."));
        assertTrue(matches(UPGRADER, "...", "FB.", "W.."));
        assertTrue(matches(UPGRADER, "...", ".FB", ".W."));
        // im 2×2-Feld des Inventars
        assertTrue(matches(UPGRADER, "FB", "W."));
        // waagrecht gespiegelt
        assertTrue(matches(UPGRADER, "BF.", ".W.", "..."));
        assertTrue(matches(UPGRADER, "BF", ".W"));
        // senkrecht gespiegelt oder gedreht passt nicht
        assertFalse(matches(UPGRADER, "W.", "FB"));
        assertFalse(matches(UPGRADER, "FW", "B."));
        // zusätzliche Zutat oder falsche Lücke
        assertFalse(matches(UPGRADER, "FB.", "WS.", "..."));
        assertFalse(matches(UPGRADER, "FB.", "...", "W.."));
        // falsche Zutat
        assertFalse(matches(UPGRADER, "FB.", "S..", "..."));
    }

    @Test
    void patternWithBlankBorderIsTrimmed() throws IOException {
        writeConfig("""
                { "recipes": { "gem_trader": {
                    "pattern": ["   ", " DD", "   "],
                    "key": { "D": "minecraft:diamond" } } } }
                """);
        assertTrue(matches(TRADER, "DD.", "...", "..."));
        assertTrue(matches(TRADER, "...", "...", ".DD"));
        assertTrue(matches(TRADER, "DD", ".."));
        assertFalse(matches(TRADER, "D..", "D..", "..."));
    }

    // ------------------------------------------------------------------ Formlos: Energy Bottle → Fragments

    @Test
    void gemFragmentsMatchOnlyWithActiveBottleInAnyGrid() {
        ItemSpec bottle = core.bottles().create("admin", "Test");
        String id = RecipeSpec.GEM_FRAGMENTS;
        for (int i = 0; i < 4; i++) {
            assertTrue(core.recipes().matches(id, 2, 2, withBottleAt(4, i, bottle.data())), "2×2, Feld " + i);
        }
        assertTrue(core.recipes().matches(id, 3, 3, withBottleAt(9, 8, bottle.data())), "3×3");

        List<Cell> twoItems = withBottleAt(4, 0, bottle.data());
        twoItems.set(3, new Cell("minecraft:stone", Map.of()));
        assertFalse(core.recipes().matches(id, 2, 2, twoItems), "nur die Flasche allein");

        List<Cell> twoBottles = withBottleAt(4, 0, bottle.data());
        twoBottles.set(1, new Cell(CustomItem.ENERGY_BOTTLE.itemId(), bottle.data()));
        assertFalse(core.recipes().matches(id, 2, 2, twoBottles), "zwei Flaschen (z. B. Kopien) passen nicht");

        assertFalse(core.recipes().matches(id, 2, 2, withBottleAt(4, 0,
                ItemCatalog.energyBottle(UUID.randomUUID()).data())), "Fälschung ohne Registereintrag");
        assertFalse(core.recipes().matches(id, 2, 2, List.of(Cell.EMPTY, Cell.EMPTY, Cell.EMPTY, Cell.EMPTY)));
        List<Cell> glass = List.of(new Cell("minecraft:glass_bottle", Map.of()), Cell.EMPTY, Cell.EMPTY, Cell.EMPTY);
        assertFalse(core.recipes().matches(id, 2, 2, glass), "Glasflasche ist keine Energy Bottle");
    }

    @Test
    void craftingFragmentsClosesBottleSoCopiesNoLongerMatch() {
        ItemSpec bottle = core.bottles().create("admin", "Test");
        List<Cell> cells = withBottleAt(4, 2, bottle.data());
        assertTrue(core.recipes().onCrafted(RecipeSpec.GEM_FRAGMENTS, cells, "Steve"));
        assertFalse(core.recipes().matches(RecipeSpec.GEM_FRAGMENTS, 2, 2, cells), "Flasche ist verbraucht");
        assertFalse(core.recipes().onCrafted(RecipeSpec.GEM_FRAGMENTS, cells, "Steve"), "Kopie liefert nichts");
        assertTrue(core.recipes().onCrafted(UPGRADER, grid("FBF", "BWB", "FBF"), "Steve"),
                "geformte Rezepte brauchen keine Prüfung beim Herausnehmen");
    }

    // ------------------------------------------------------------------ Abschalten

    @Test
    void disabledRecipesDoNotMatch() throws IOException {
        writeConfig("""
                { "recipes": { "gem_trader": { "enabled": false }, "gem_fragments": { "enabled": false } } }
                """);
        assertFalse(matches(TRADER, "DBD", "BFB", "DBD"));
        ItemSpec bottle = core.bottles().create("admin", "Test");
        assertFalse(core.recipes().matches(RecipeSpec.GEM_FRAGMENTS, 2, 2, withBottleAt(4, 0, bottle.data())));
        assertTrue(matches(UPGRADER, "FBF", "BWB", "FBF"), "nicht genannte Rezepte behalten ihre Standardwerte");
    }

    @Test
    void invalidConfigRecipesAreDisabledWithWarning() throws IOException {
        List<String> messages = writeConfig("""
                { "recipes": {
                    "upgrader":   { "pattern": ["FXF"], "key": { "F": "xmassmp:gem_fragment" } },
                    "gem_trader": { "pattern": ["DDDD"], "key": { "D": "minecraft:diamond" } },
                    "repair_kit": { "pattern": ["DD"], "key": { "D": "Diamond Block" } },
                    "gem_fragments": { "count": 0 }
                } }
                """);
        assertWarning(messages, "recipes.upgrader", "'X'");
        assertWarning(messages, "recipes.gem_trader", "1–3 Zeichen");
        assertWarning(messages, "recipes.repair_kit", "key");
        assertWarning(messages, "recipes.gem_fragments", "count");

        assertFalse(core.config().get().recipes.get(UPGRADER).enabled);
        assertFalse(matches(UPGRADER, "FBF", "BWB", "FBF"), "abgeschaltet, auch das alte Muster passt nicht mehr");
        assertFalse(matches(UPGRADER, "F.F", "...", "..."));
        assertFalse(matches(TRADER, "DBD", "BFB", "DBD"));
        assertFalse(matches(REPAIR_KIT, "FAF", "KTK", "FAF"));
        ItemSpec bottle = core.bottles().create("admin", "Test");
        assertFalse(core.recipes().matches(RecipeSpec.GEM_FRAGMENTS, 2, 2, withBottleAt(4, 0, bottle.data())));
    }

    @Test
    void tooManyRowsAreRejected() throws IOException {
        List<String> messages = writeConfig("""
                { "recipes": { "upgrader": { "pattern": ["F", "F", "F", "F"], "key": { "F": "xmassmp:gem_fragment" } } } }
                """);
        assertWarning(messages, "recipes.upgrader", "3 Zeilen");
        assertFalse(core.config().get().recipes.get(UPGRADER).enabled);
    }

    private static void assertWarning(List<String> messages, String recipe, String detail) {
        assertTrue(messages.stream().anyMatch(m -> m.startsWith(recipe + ":") && m.contains(detail)
                        && m.contains("abgeschaltet")),
                "Warnung für " + recipe + " (" + detail + ") fehlt: " + messages);
    }
}
