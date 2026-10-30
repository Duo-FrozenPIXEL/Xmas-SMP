package de.xmassmp.gui;

import java.util.UUID;

/** Öffnet Kisten-GUIs auf der Plattform. */
public interface GuiHost {

    /**
     * Öffnet {@code view} für den Spieler oder ersetzt seine offene GUI.
     *
     * @return {@code false}, wenn der Spieler offline ist oder die Plattform keine GUIs kann
     */
    boolean open(UUID player, GuiView view);

    void close(UUID player);

    /** Für Plattformen bzw. Tests ohne GUI-Unterstützung. */
    GuiHost UNSUPPORTED = new GuiHost() {
        @Override
        public boolean open(UUID player, GuiView view) {
            return false;
        }

        @Override
        public void close(UUID player) {
        }
    };
}
