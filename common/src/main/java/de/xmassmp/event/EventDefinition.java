package de.xmassmp.event;

import de.xmassmp.event.action.EventAction;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Ein Event aus {@code events.jsonc}.
 *
 * @param duration         Dauer; {@link Duration#ZERO} = punktuelles Event (nur Start-Aktionen)
 * @param announceMinutes  Ankündigungen so viele Minuten vor Start
 * @param countdownSeconds Sekunden-Countdown vor Start (0 = keiner)
 * @param modifiers        globale Multiplikatoren, solange das Event läuft (z. B. {@code energy_multiplier})
 */
public record EventDefinition(
        String id,
        String name,
        boolean enabled,
        EventSchedule schedule,
        Duration duration,
        List<Integer> announceMinutes,
        int countdownSeconds,
        List<EventAction> onStart,
        List<EventAction> onEnd,
        Map<String, Double> modifiers) {

    public EventDefinition {
        announceMinutes = announceMinutes.stream().filter(m -> m > 0).distinct().sorted().toList();
        onStart = List.copyOf(onStart);
        onEnd = List.copyOf(onEnd);
        modifiers = Map.copyOf(modifiers);
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Dauer darf nicht negativ sein");
        }
        if (countdownSeconds < 0) {
            throw new IllegalArgumentException("countdownSeconds darf nicht negativ sein");
        }
    }

    /** Größter Vorlauf vor dem Start (Ankündigung oder Countdown). */
    public Duration lead() {
        long minutes = announceMinutes.isEmpty() ? 0 : announceMinutes.get(announceMinutes.size() - 1);
        return Duration.ofMinutes(minutes).plusSeconds(countdownSeconds);
    }
}
