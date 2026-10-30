package de.xmassmp.gui;

import java.util.List;
import java.util.function.Consumer;

/**
 * Ein Feld einer Kisten-GUI.
 *
 * @param icon    Item-ID, z. B. {@code minecraft:lime_dye}
 * @param name    Anzeigename mit {@code §}-Farbcodes
 * @param lore    Zeilen unter dem Namen
 * @param glow    Verzauberungs-Schimmer
 * @param onClick Reaktion auf einen Klick oder {@code null} für reine Anzeige
 */
public record GuiButton(String icon, String name, List<String> lore, boolean glow, Consumer<GuiClick> onClick) {

    public GuiButton {
        lore = List.copyOf(lore);
    }

    /** Reine Anzeige ohne Klick-Reaktion. */
    public static GuiButton label(String icon, String name, String... lore) {
        return new GuiButton(icon, name, List.of(lore), false, null);
    }

    public static GuiButton action(String icon, String name, Consumer<GuiClick> onClick, String... lore) {
        return new GuiButton(icon, name, List.of(lore), false, onClick);
    }

    public GuiButton withGlow(boolean value) {
        return new GuiButton(icon, name, lore, value, onClick);
    }

    /** Führt den Klick aus; ohne Reaktion passiert nichts. */
    public void click(GuiClick click) {
        if (onClick != null) {
            onClick.accept(click);
        }
    }
}
