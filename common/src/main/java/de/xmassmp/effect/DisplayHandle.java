package de.xmassmp.effect;

import de.xmassmp.util.Vec3;

/** Steuerung eines gespawnten Display-Entities. Nach {@link #remove()} sind alle Aufrufe wirkungslos. */
public interface DisplayHandle {

    /** Bewegt das Display, über {@code ticks} weich interpoliert (0 = sofort). */
    void moveTo(Vec3 position, int ticks);

    /** Ändert Größe/Drehung/Verschiebung, über {@code ticks} weich interpoliert (0 = sofort). */
    void transform(DisplayTransform transform, int ticks);

    void remove();

    boolean isRemoved();

    /** Handle ohne Wirkung, z. B. wenn das Display-Budget erschöpft ist. */
    static DisplayHandle none() {
        return new DisplayHandle() {
            private boolean removed;

            @Override
            public void moveTo(Vec3 position, int ticks) {
            }

            @Override
            public void transform(DisplayTransform transform, int ticks) {
            }

            @Override
            public void remove() {
                removed = true;
            }

            @Override
            public boolean isRemoved() {
                return removed;
            }
        };
    }
}
