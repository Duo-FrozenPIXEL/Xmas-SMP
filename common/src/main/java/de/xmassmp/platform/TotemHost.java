package de.xmassmp.platform;

import de.xmassmp.util.BlockPos;

/**
 * Setzt und entfernt den Totem-Block des Repair Kits. Der Block ist für Vanilla unzerstörbar
 * (Explosionen, Kolben, Feuer); Abbauen dauert immer {@code totem.breakSeconds}. Fortschritt und
 * Bruch meldet der Adapter an {@link de.xmassmp.totem.TotemService}.
 */
public interface TotemHost {

    /** @return {@code false}, wenn die Stelle nicht frei ist oder die Welt nicht geladen ist */
    boolean place(WorldRef world, BlockPos pos);

    boolean isPresent(WorldRef world, BlockPos pos);

    /** Entfernt den Block ohne Drops; nichts passiert, wenn er schon weg ist. */
    void remove(WorldRef world, BlockPos pos);

    TotemHost UNSUPPORTED = new TotemHost() {
        @Override
        public boolean place(WorldRef world, BlockPos pos) {
            return false;
        }

        @Override
        public boolean isPresent(WorldRef world, BlockPos pos) {
            return false;
        }

        @Override
        public void remove(WorldRef world, BlockPos pos) {
        }
    };
}
