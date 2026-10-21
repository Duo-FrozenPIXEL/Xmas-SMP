package de.xmassmp.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagesTest {

    @Test
    void colorizeConvertsOnlyValidCodes() {
        assertEquals("§cRot §lfett", Messages.colorize("&cRot &Lfett"));
        assertEquals("A & B", Messages.colorize("A & B"));
        assertEquals("&c", Messages.colorize("&&c"));
        assertEquals("Ende&", Messages.colorize("Ende&"));
        assertEquals("&x", Messages.colorize("&x"));
    }

    @Test
    void localizedTextFormatsNestedArgs() {
        LocalizedText text = LocalizedText.of("a", "%s-Kristall")
                .withArgs(LocalizedText.of("b", "Feuer"));
        assertEquals("Feuer-Kristall", text.toPlainString());
    }
}
