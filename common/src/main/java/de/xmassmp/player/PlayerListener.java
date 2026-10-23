package de.xmassmp.player;

import de.xmassmp.platform.PlayerHandle;

import java.util.Map;
import java.util.UUID;

/**
 * Reagiert auf Spieler-Ereignisse, die der Plattform-Adapter an {@link de.xmassmp.XmasCore}
 * meldet. Registrierung über {@link de.xmassmp.XmasCore#addPlayerListener(PlayerListener)}.
 *
 * <p>Alle Methoden laufen auf dem Server-Thread. Bei {@link #onSwapHands} und {@link #onUseItem}
 * gewinnt der erste Listener, der {@code true} liefert; die übrigen werden nicht mehr gefragt.
 */
public interface PlayerListener {

    default void onJoin(PlayerHandle player) {
    }

    default void onQuit(UUID uuid) {
    }

    /**
     * Ein Spieler ist gestorben.
     *
     * @param killer Spieler, der den Tod verursacht hat, oder {@code null} (Mob, Umwelt, Selbstmord)
     */
    default void onDeath(PlayerHandle victim, PlayerHandle killer) {
    }

    default void onRespawn(PlayerHandle player) {
    }

    /**
     * Taste „Item mit Zweithand tauschen“ (Standard {@code F}).
     *
     * @return {@code true}, um den Tausch abzufangen
     */
    default boolean onSwapHands(PlayerHandle player) {
        return false;
    }

    /**
     * Rechtsklick mit einem Custom-Item in der Hand.
     *
     * @param itemData Markierungen aus {@code custom_data.xmassmp}, z. B. {@code type}
     * @return {@code true}, wenn die Nutzung behandelt wurde (Vanilla-Verhalten entfällt)
     */
    default boolean onUseItem(PlayerHandle player, Map<String, String> itemData) {
        return false;
    }
}
