package de.xmassmp.event;

/**
 * Texte für automatische Ankündigungen ({@code &}-Farbcodes, Platzhalter {@code {name}},
 * {@code {minutes}}, {@code {seconds}}). Leerer Text = nichts senden.
 */
public record EventMessages(
        String announce,
        String announceSound,
        String countdownTitle,
        String countdownSubtitle,
        String countdownSound) {

    public static final EventMessages DEFAULTS = new EventMessages(
            "&6✦ &e{name} &6beginnt in &e{minutes} &6Minute(n)!",
            "minecraft:block.note_block.bell",
            "&c{seconds}",
            "&e{name}",
            "minecraft:block.note_block.hat");
}
