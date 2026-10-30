package de.xmassmp.gui;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inhalt einer Kisten-GUI mit 1–6 Reihen zu je 9 Feldern. Der Adapter zeigt sie z. B. über
 * sgui an; der Kern beschreibt nur, was wo liegt und was ein Klick bewirkt.
 *
 * @param title   Titel mit {@code §}-Farbcodes
 * @param buttons Feld-Index (0 = oben links) → Inhalt; leere Felder fehlen
 */
public record GuiView(String title, int rows, Map<Integer, GuiButton> buttons) {

    public static final int COLUMNS = 9;

    public GuiView {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("GUI braucht 1–6 Reihen: " + rows);
        }
        for (int slot : buttons.keySet()) {
            if (slot < 0 || slot >= rows * COLUMNS) {
                throw new IllegalArgumentException("Feld " + slot + " liegt außerhalb von " + rows + " Reihen");
            }
        }
        buttons = Map.copyOf(buttons);
    }

    public static Builder builder(String title, int rows) {
        return new Builder(title, rows);
    }

    public GuiButton button(int slot) {
        return buttons.get(slot);
    }

    public static final class Builder {
        private final String title;
        private final int rows;
        private final Map<Integer, GuiButton> buttons = new LinkedHashMap<>();

        private Builder(String title, int rows) {
            this.title = title;
            this.rows = rows;
        }

        public Builder set(int slot, GuiButton button) {
            buttons.put(slot, button);
            return this;
        }

        public Builder set(int row, int column, GuiButton button) {
            return set(row * COLUMNS + column, button);
        }

        public int rows() {
            return rows;
        }

        public GuiView build() {
            return new GuiView(title, rows, buttons);
        }
    }
}
