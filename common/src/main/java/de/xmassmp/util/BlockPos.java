package de.xmassmp.util;

/** Ganzzahlige Blockposition. */
public record BlockPos(int x, int y, int z) {

    public static BlockPos of(Vec3 position) {
        return new BlockPos((int) Math.floor(position.x()), (int) Math.floor(position.y()),
                (int) Math.floor(position.z()));
    }

    /** Mitte der Blockoberseite. */
    public Vec3 topCenter() {
        return new Vec3(x + 0.5, y + 1, z + 0.5);
    }

    public Vec3 center() {
        return new Vec3(x + 0.5, y + 0.5, z + 0.5);
    }

    @Override
    public String toString() {
        return x + " " + y + " " + z;
    }
}
