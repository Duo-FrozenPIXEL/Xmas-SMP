package de.xmassmp.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Ein Werkbank-Rezept aus {@code config.jsonc} ({@code recipes.<id>}). Geformt über {@code pattern}
 * (1–3 Zeilen à 1–3 Zeichen, Leerzeichen = leeres Feld) und {@code key}; das Ergebnis ist das
 * Custom-Item mit der Rezept-ID. Nur {@code gem_fragments} ist formlos und nutzt {@code count}.
 *
 * <p>Zutaten sind Item-IDs, z. B. {@code minecraft:diamond_block} oder {@code xmassmp:gem_fragment}.
 */
public final class RecipeSpec {

    /** Formloses Rezept: 1 Energy Bottle → {@code count} Gem Fragments. */
    public static final String GEM_FRAGMENTS = "gem_fragments";

    private static final Pattern ITEM_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public boolean enabled = true;
    public List<String> pattern = new ArrayList<>();
    public Map<String, String> key = new LinkedHashMap<>();
    /** Nur {@code gem_fragments}: Anzahl Fragments pro Flasche. */
    public int count = 9;

    static RecipeSpec shaped(String[] rows, String... keyPairs) {
        RecipeSpec spec = new RecipeSpec();
        spec.pattern = new ArrayList<>(List.of(rows));
        for (int i = 0; i < keyPairs.length; i += 2) {
            spec.key.put(keyPairs[i], keyPairs[i + 1]);
        }
        return spec;
    }

    static RecipeSpec disabled() {
        RecipeSpec spec = new RecipeSpec();
        spec.enabled = false;
        return spec;
    }

    /**
     * Standardrezepte nach den Vorgaben des Owners: Original-Rezepte, Fragments nur aus Energy
     * Bottles, Repair Kit mit Diamantblöcken statt Netherit, Trader günstig mit Diamanten.
     */
    public static Map<String, RecipeSpec> defaults() {
        Map<String, RecipeSpec> out = new LinkedHashMap<>();
        out.put("upgrader", shaped(new String[]{"FBF", "BWB", "FBF"},
                "F", "xmassmp:gem_fragment",
                "B", "minecraft:dragon_breath",
                "W", "minecraft:wither_skeleton_skull"));
        out.put("gem_trader", shaped(new String[]{"DBD", "BFB", "DBD"},
                "D", "minecraft:diamond",
                "B", "minecraft:dragon_breath",
                "F", "xmassmp:gem_fragment"));
        out.put("repair_kit", shaped(new String[]{"FAF", "DTD", "FAF"},
                "F", "xmassmp:gem_fragment",
                "A", "minecraft:anvil",
                "D", "minecraft:diamond_block",
                "T", "minecraft:netherite_upgrade_smithing_template"));
        out.put(GEM_FRAGMENTS, new RecipeSpec());
        return out;
    }

    /** Beschreibung des ersten Fehlers oder {@code null}, wenn das Rezept gültig ist. */
    String problem() {
        if (!enabled) {
            return null;
        }
        if (count < 1 || count > 64) {
            return "count muss 1–64 sein";
        }
        if (pattern == null || key == null) {
            return "pattern/key fehlt";
        }
        if (pattern.isEmpty()) {
            return null;
        }
        if (pattern.size() > 3) {
            return "höchstens 3 Zeilen";
        }
        for (String row : pattern) {
            if (row == null || row.isEmpty() || row.length() > 3) {
                return "jede Zeile braucht 1–3 Zeichen";
            }
            for (char c : row.toCharArray()) {
                if (c != ' ' && !key.containsKey(String.valueOf(c))) {
                    return "Zeichen '" + c + "' fehlt in key";
                }
            }
        }
        for (Map.Entry<String, String> entry : key.entrySet()) {
            if (entry.getKey().length() != 1 || entry.getValue() == null || !ITEM_ID.matcher(entry.getValue()).matches()) {
                return "ungültiger key-Eintrag '" + entry.getKey() + "'";
            }
        }
        return null;
    }
}
