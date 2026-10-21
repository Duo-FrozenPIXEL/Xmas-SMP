package de.xmassmp.event;

import java.util.ArrayList;
import java.util.List;

/** Merkt sich alle Ausgaben als lesbare Zeilen. */
final class RecordingHost implements EventHost {

    final List<String> log = new ArrayList<>();

    @Override
    public void broadcast(String legacyText) {
        log.add("broadcast:" + legacyText);
    }

    @Override
    public void title(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        log.add("title:" + title + "|" + subtitle);
    }

    @Override
    public void playSound(String sound, float volume, float pitch) {
        log.add("sound:" + sound);
    }

    @Override
    public void runCommand(String command) {
        log.add("command:" + command);
    }

    List<String> without(String prefix) {
        return log.stream().filter(line -> !line.startsWith(prefix)).toList();
    }
}
