package de.xmassmp.crystal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.xmassmp.item.CustomItem;
import de.xmassmp.item.ItemCatalog;
import de.xmassmp.item.ItemSpec;
import de.xmassmp.text.LocalizedText;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gleicht Code-Konstanten mit dem Ressourcenpaket ab, damit beides nicht auseinanderläuft. */
class CrystalPackTest {

    private static final Path PACK = Path.of(System.getProperty("xmassmp.resourcepack", "../resourcepack"));

    @Test
    void customModelDataFollowsDocumentedFormula() {
        assertEquals(2047, CrystalModels.customModelData(Crystals.FIRE, 2, PristineStage.PRISTINE));
        assertEquals(1002, CrystalModels.customModelData(Crystals.EARTH, 1, PristineStage.BROKEN));
        assertEquals("xmassmp:item/strength_gem_tier2_pristine4",
                CrystalModels.modelId(Crystals.FIRE, 2, PristineStage.PRISTINE));
    }

    @Test
    void everyCrystalVariantIsMappedInPack() throws IOException {
        Map<Integer, String> mapping = rangeDispatch("echo_shard");
        for (Crystal crystal : Crystals.DEFAULTS) {
            for (int tier = CrystalModels.MIN_TIER; tier <= CrystalModels.MAX_TIER; tier++) {
                for (PristineStage stage : PristineStage.values()) {
                    int cmd = CrystalModels.customModelData(crystal, tier, stage);
                    String model = CrystalModels.modelId(crystal, tier, stage);
                    assertEquals(model, mapping.get(cmd), crystal.id() + " T" + tier + " " + stage);
                    assertTrue(Files.exists(modelFile(model)), "Modell fehlt: " + model);
                }
            }
        }
    }

    @Test
    void customItemsMatchPack() throws IOException {
        for (CustomItem item : CustomItem.values()) {
            String base = item.baseItem().substring("minecraft:".length());
            Map<Integer, String> mapping = rangeDispatch(base);
            assertTrue(mapping.containsKey(item.customModelData()),
                    item.id() + ": CMD " + item.customModelData() + " fehlt in items/" + base + ".json");
            assertEquals("xmassmp:item/" + item.id(), mapping.get(item.customModelData()));
        }
    }

    @Test
    void hudIconsPointToCrossMappedTextures() throws IOException {
        JsonObject font = read(PACK.resolve("assets/minecraft/font/default.json")).getAsJsonObject();
        Map<String, String> fileByChar = new HashMap<>();
        for (JsonElement provider : font.getAsJsonArray("providers")) {
            JsonObject p = provider.getAsJsonObject();
            fileByChar.put(p.getAsJsonArray("chars").get(0).getAsString(), p.get("file").getAsString());
        }
        // todo.md, Abschnitt 0: Feuer bekommt die Feuer-Icons, Erde die Strength-Icons usw.
        Map<Crystal, String> expected = Map.of(
                Crystals.FIRE, "fire", Crystals.EARTH, "strength", Crystals.WIND, "puff",
                Crystals.SHADOW, "astra", Crystals.NATURE, "wealth");
        expected.forEach((crystal, icon) -> {
            assertEquals("xmassmp:icons/" + icon + "1.png", fileByChar.get(crystal.hudIcon(1)), crystal.id());
            assertEquals("xmassmp:icons/" + icon + "2.png", fileByChar.get(crystal.hudIcon(2)), crystal.id());
        });
    }

    @Test
    void allTranslationKeysExistInBothLanguages() throws IOException {
        List<LocalizedText> texts = new ArrayList<>();
        for (Crystal crystal : Crystals.DEFAULTS) {
            ItemSpec spec = ItemCatalog.crystal(crystal, 1, PristineStage.BROKEN);
            texts.add(spec.name());
            texts.addAll(spec.lore());
            texts.addAll(ItemCatalog.ownedCrystal(crystal, 2, PristineStage.PRISTINE, 3, 10,
                    java.util.UUID.randomUUID(), true).lore());
        }
        for (PristineStage stage : PristineStage.values()) {
            texts.add(stage.displayName());
        }
        for (CustomItem item : CustomItem.values()) {
            texts.add(ItemCatalog.custom(item).name());
        }
        for (String lang : List.of("de_de", "en_us")) {
            JsonObject json = read(PACK.resolve("assets/xmassmp/lang/" + lang + ".json")).getAsJsonObject();
            for (LocalizedText text : texts) {
                assertKeys(json, text, lang);
            }
        }
    }

    @Test
    void germanTranslationsMatchFallbacks() throws IOException {
        JsonObject de = read(PACK.resolve("assets/xmassmp/lang/de_de.json")).getAsJsonObject();
        for (Crystal crystal : Crystals.DEFAULTS) {
            assertEquals(crystal.nameFallback(), de.get(crystal.displayName().key()).getAsString());
        }
        for (CustomItem item : CustomItem.values()) {
            assertEquals(item.displayName().fallback(), de.get(item.displayName().key()).getAsString());
        }
    }

    @Test
    void registryRejectsDuplicateTexture() {
        CrystalRegistry registry = CrystalRegistry.withDefaults();
        Crystal copy = new Crystal("ice", "Eis", "strength", 7, Crystals.FIRE.color(),
                Crystals.FIRE.accentColor(), 'b', "");
        assertThrows(IllegalArgumentException.class, () -> registry.register(copy));
    }

    private static void assertKeys(JsonObject lang, LocalizedText text, String langName) {
        if (!text.isLiteral()) {
            assertTrue(lang.has(text.key()), langName + ": Schlüssel fehlt: " + text.key());
        }
        text.args().forEach(arg -> assertKeys(lang, arg, langName));
    }

    private static Map<Integer, String> rangeDispatch(String item) throws IOException {
        JsonObject root = read(PACK.resolve("assets/minecraft/items/" + item + ".json")).getAsJsonObject();
        Map<Integer, String> out = new HashMap<>();
        for (JsonElement entry : root.getAsJsonObject("model").getAsJsonArray("entries")) {
            JsonObject e = entry.getAsJsonObject();
            out.put(e.get("threshold").getAsInt(), e.getAsJsonObject("model").get("model").getAsString());
        }
        return out;
    }

    private static Path modelFile(String modelId) {
        String[] parts = modelId.split(":", 2);
        return PACK.resolve("assets/" + parts[0] + "/models/" + parts[1] + ".json");
    }

    private static JsonElement read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file));
    }
}
