package de.xmassmp.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsoncEditorTest {

    @TempDir
    Path dir;

    private static final String TEXT = """
            // Kopf
            {
              "general": {
                // Hauptschalter
                "enabled": true, # Kommentar hinter dem Wert
                "name": "a, b } c"
              },
              "crystals": {
                "fire": { "enabled": true, "cooldownMultiplier": 1.0 }
              },
              "events": [
                { "name": "x", "id": "eve", "enabled": false },
                { "id": "new_year" }
              ]
            }
            """;

    @Test
    void replacesValueAndKeepsEverythingElse() {
        String out = JsoncEditor.set(TEXT, List.of("general", "enabled"), "false");
        assertEquals(TEXT.replace("\"enabled\": true, #", "\"enabled\": false, #"), out);
    }

    @Test
    void handlesStringsWithBracesAndInlineObjects() {
        String out = JsoncEditor.set(TEXT, List.of("crystals", "fire", "cooldownMultiplier"), "0.5");
        assertTrue(out.contains("{ \"enabled\": true, \"cooldownMultiplier\": 0.5 }"), out);
        assertTrue(out.contains("\"name\": \"a, b } c\""));
    }

    @Test
    void insertsMissingKeys() {
        String inline = JsoncEditor.set(TEXT, List.of("crystals", "fire", "damageMultiplier"), "2.0");
        assertTrue(inline.contains("{ \"damageMultiplier\": 2.0, \"enabled\": true"), inline);

        String block = JsoncEditor.set(TEXT, List.of("crystals", "wind", "enabled"), "false");
        assertTrue(block.contains("\"wind\": { \"enabled\": false },\n    \"fire\""), block);

        String root = JsoncEditor.set("{}", List.of("hud", "enabled"), "true");
        assertEquals("{ \"hud\": { \"enabled\": true } }", root);
    }

    @Test
    void findsArrayElementById() {
        String out = JsoncEditor.set(TEXT, List.of("events", "id=eve", "enabled"), "true");
        assertTrue(out.contains("{ \"name\": \"x\", \"id\": \"eve\", \"enabled\": true }"), out);
        String added = JsoncEditor.set(TEXT, List.of("events", "id=new_year", "enabled"), "false");
        assertTrue(added.contains("{ \"enabled\": false, \"id\": \"new_year\" }"), added);
        assertThrows(IllegalArgumentException.class,
                () -> JsoncEditor.set(TEXT, List.of("events", "id=missing", "enabled"), "true"));
    }

    @Test
    void rejectsBrokenInput() {
        assertThrows(IllegalArgumentException.class, () -> JsoncEditor.set("{ \"a\": ", List.of("a"), "1"));
        assertThrows(IllegalArgumentException.class, () -> JsoncEditor.set(TEXT, List.of("general", "enabled", "x"), "1"));
    }

    @Test
    void literals() {
        assertEquals("true", JsoncEditor.literal(true));
        assertEquals("5", JsoncEditor.literal(5));
        assertEquals("1.0", JsoncEditor.literal(1.0));
        assertEquals("0.35", JsoncEditor.literal(0.35));
        assertEquals("\"a\\\"b\"", JsoncEditor.literal("a\"b"));
        assertThrows(IllegalArgumentException.class, () -> JsoncEditor.literal(Double.NaN));
    }

    @Test
    void configManagerSetKeepsTemplateCommentsAndAppliesValue() throws IOException {
        ConfigManager config = new ConfigManager(dir);
        config.load();
        ConfigManager.LoadResult result = config.set(List.of("crystals", "fire", "cooldownMultiplier"), 0.5);
        assertTrue(result.success(), result.messages().toString());
        assertEquals(0.5, config.get().crystal("fire").cooldownMultiplier);
        assertTrue(config.set(List.of("general", "enabled"), false).success());
        assertFalse(config.get().general.enabled);

        String text = Files.readString(dir.resolve(ConfigManager.FILE_NAME));
        assertTrue(text.contains("// Hauptschalter für das ganze Plugin."), "Kommentare bleiben erhalten");
        assertTrue(text.contains("\"enabled\": false"));
    }
}
