package de.xmassmp.event;

/** Was Event-Aktionen auf dem Server auslösen können. Implementiert vom Plattform-Adapter. */
public interface EventHost {

    /** Chat-Nachricht an alle (Legacy-Farbcodes mit §). */
    void broadcast(String legacyText);

    /** Titel an alle. Zeiten in Ticks. */
    void title(String title, String subtitle, int fadeIn, int stay, int fadeOut);

    /** Sound für alle Spieler an ihrer eigenen Position. */
    void playSound(String sound, float volume, float pitch);

    /** Befehl als Konsole ausführen (ohne führenden Schrägstrich). */
    void runCommand(String command);
}
