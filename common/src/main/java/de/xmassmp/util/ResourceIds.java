package de.xmassmp.util;

import java.util.regex.Pattern;

/**
 * Prüft IDs wie {@code minecraft:block.note_block.bell} nach den Regeln von Minecraft, damit
 * Tippfehler beim Laden auffallen und nicht erst zur Laufzeit eine Exception werfen.
 */
public final class ResourceIds {

    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");

    private ResourceIds() {
    }

    /** {@code namespace:pfad} oder nur {@code pfad} (Namespace dann {@code minecraft}). */
    public static boolean isValid(String id) {
        if (id == null) {
            return false;
        }
        int colon = id.indexOf(':');
        if (colon < 0) {
            return PATH.matcher(id).matches();
        }
        return NAMESPACE.matcher(id.substring(0, colon)).matches()
                && PATH.matcher(id.substring(colon + 1)).matches();
    }
}
