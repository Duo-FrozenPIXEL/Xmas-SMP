package de.xmassmp.gui;

/** Art eines Klicks in einer Kisten-GUI. */
public enum GuiClick {
    LEFT,
    RIGHT,
    SHIFT_LEFT,
    SHIFT_RIGHT;

    public boolean right() {
        return this == RIGHT || this == SHIFT_RIGHT;
    }

    public boolean shift() {
        return this == SHIFT_LEFT || this == SHIFT_RIGHT;
    }
}
