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

    @Test
    void fallbackWithPercentSignDoesNotThrow() {
        LocalizedText text = LocalizedText.of("c", "50% schneller")
                .withArgs(LocalizedText.of("d", "Feuer"));
        assertEquals("50% schneller Feuer", text.toPlainString());
    }

    @Test
    void percentSFallbackStillReplacesArgument() {
        LocalizedText text = LocalizedText.of("e", "Kristall: %s")
                .withArgs(LocalizedText.literal("Wind"));
        assertEquals("Kristall: Wind", text.toPlainString());
    }
}
