package de.xmassmp.recipe;

import de.xmassmp.bottle.BottleService;
import de.xmassmp.config.RecipeSpec;
import de.xmassmp.config.XmasConfig;
import de.xmassmp.item.CustomItem;
import de.xmassmp.item.ItemCatalog;
import de.xmassmp.item.ItemSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Werkbank-Rezepte aus {@code config.jsonc} ({@code recipes}). Der Adapter meldet jedes Feld als
 * {@link Cell}; geformte Rezepte passen wie in Vanilla an jeder Stelle des Gitters und gespiegelt.
 *
 * <p>Das formlose Rezept {@link RecipeSpec#GEM_FRAGMENTS} (1 Energy Bottle → Fragments) zeigt nur für
 * aktive Flaschen ein Ergebnis und schließt die Flasche beim Herausnehmen ({@link #onCrafted}).
 */
public final class RecipeService {

    /** Ein Feld des Werkbank-Gitters; leer = {@code minecraft:air}. */
    public record Cell(String itemId, Map<String, String> data) {

        public static final Cell EMPTY = new Cell("minecraft:air", Map.of());

        public Cell {
            data = Map.copyOf(data);
        }

        public boolean isEmpty() {
            return itemId == null || itemId.equals("minecraft:air");
        }
    }

    /** Ergebnis eines Rezepts. */
    public record Result(ItemSpec item, int count) {
    }

    private final Supplier<XmasConfig> config;
    private final BottleService bottles;

    public RecipeService(Supplier<XmasConfig> config, BottleService bottles) {
        this.config = config;
        this.bottles = bottles;
    }

    /** Rezept-IDs, für die der Adapter je ein Werkbank-Rezept anmeldet. */
    public static List<String> ids() {
        List<String> out = new ArrayList<>();
        out.add(CustomItem.UPGRADER.id());
        out.add(CustomItem.GEM_TRADER.id());
        out.add(CustomItem.REPAIR_KIT.id());
        out.add(RecipeSpec.GEM_FRAGMENTS);
        return out;
    }

    /** Passt das Gitter ({@code width}×{@code height}, zeilenweise) zum Rezept {@code id}? */
    public boolean matches(String id, int width, int height, List<Cell> cells) {
        RecipeSpec spec = config.get().recipes.get(id);
        if (spec == null || !spec.enabled || cells.size() != width * height) {
            return false;
        }
        if (RecipeSpec.GEM_FRAGMENTS.equals(id)) {
            Cell bottle = singleItem(cells);
            return bottle != null && CustomItem.ENERGY_BOTTLE.itemId().equals(bottle.itemId())
                    && bottles.canCraft(bottle.data());
        }
        return matchesShaped(spec.pattern, spec.key, width, height, cells);
    }

    public Optional<Result> result(String id) {
        RecipeSpec spec = config.get().recipes.get(id);
        if (spec == null) {
            return Optional.empty();
        }
        if (RecipeSpec.GEM_FRAGMENTS.equals(id)) {
            return Optional.of(new Result(ItemCatalog.custom(CustomItem.GEM_FRAGMENT), spec.count));
        }
        return CustomItem.byId(id).map(item -> new Result(ItemCatalog.custom(item), 1));
    }

    /**
     * Beim Herausnehmen des Ergebnisses. Für Fragments wird die Flasche im Register geschlossen.
     *
     * @return {@code false}, wenn das Ergebnis nicht hätte entstehen dürfen
     */
    public boolean onCrafted(String id, List<Cell> cells, String crafter) {
        if (!RecipeSpec.GEM_FRAGMENTS.equals(id)) {
            return true;
        }
        Cell bottle = singleItem(cells);
        return bottle != null && bottles.consumeForCraft(bottle.data(), crafter);
    }

    private static Cell singleItem(List<Cell> cells) {
        Cell found = null;
        for (Cell cell : cells) {
            if (cell.isEmpty()) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = cell;
        }
        return found;
    }

    static boolean matchesShaped(List<String> pattern, Map<String, String> key, int width, int height,
                                 List<Cell> cells) {
        if (pattern.isEmpty()) {
            return false;
        }
        int[] box = boundingBox(width, height, cells);
        if (box == null) {
            return false;
        }
        List<String> rows = trim(pattern);
        int rowsHigh = rows.size();
        int rowsWide = rows.stream().mapToInt(String::length).max().orElse(0);
        if (box[2] != rowsWide || box[3] != rowsHigh) {
            return false;
        }
        return fits(rows, key, width, cells, box, false) || fits(rows, key, width, cells, box, true);
    }

    private static boolean fits(List<String> rows, Map<String, String> key, int width, List<Cell> cells, int[] box,
                                boolean mirrored) {
        for (int y = 0; y < box[3]; y++) {
            for (int x = 0; x < box[2]; x++) {
                String row = rows.get(y);
                int px = mirrored ? box[2] - 1 - x : x;
                char symbol = px < row.length() ? row.charAt(px) : ' ';
                Cell cell = cells.get((box[1] + y) * width + box[0] + x);
                if (symbol == ' ') {
                    if (!cell.isEmpty()) {
                        return false;
                    }
                } else if (cell.isEmpty() || !cell.itemId().equals(key.get(String.valueOf(symbol)))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** {minX, minY, breite, höhe} der belegten Felder oder {@code null}, wenn alles leer ist. */
    private static int[] boundingBox(int width, int height, List<Cell> cells) {
        int minX = width, minY = height, maxX = -1, maxY = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!cells.get(y * width + x).isEmpty()) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < 0 ? null : new int[]{minX, minY, maxX - minX + 1, maxY - minY + 1};
    }

    /** Entfernt leere Rand-Zeilen und -Spalten aus dem Muster. */
    private static List<String> trim(List<String> pattern) {
        int top = 0, bottom = pattern.size() - 1;
        while (top <= bottom && pattern.get(top).isBlank()) {
            top++;
        }
        while (bottom >= top && pattern.get(bottom).isBlank()) {
            bottom--;
        }
        int left = Integer.MAX_VALUE, right = -1;
        for (int i = top; i <= bottom; i++) {
            String row = pattern.get(i);
            for (int x = 0; x < row.length(); x++) {
                if (row.charAt(x) != ' ') {
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                }
            }
        }
        List<String> out = new ArrayList<>();
        for (int i = top; i <= bottom; i++) {
            String row = pattern.get(i);
            StringBuilder trimmed = new StringBuilder();
            for (int x = left; x <= right; x++) {
                trimmed.append(x < row.length() ? row.charAt(x) : ' ');
            }
            out.add(trimmed.toString());
        }
        return out;
    }
}
