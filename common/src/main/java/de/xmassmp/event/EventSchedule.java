package de.xmassmp.event;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Wann ein Event startet: an allen Tagen, die zu {@code dates} (leer = jeden Tag) und
 * {@code weekdays} (leer = alle) passen, jeweils zu allen {@code times}.
 *
 * <p>Sommerzeit: Eine Uhrzeit in der übersprungenen Stunde (z. B. 02:30 Ende März) wird um die
 * Lücke nach hinten verschoben; in der doppelten Stunde (Ende Oktober) gilt die erste.
 * So startet jedes Event an jedem passenden Tag genau einmal pro Uhrzeit.
 */
public record EventSchedule(List<DateRule> dates, Set<DayOfWeek> weekdays, List<LocalTime> times) {

    public EventSchedule {
        dates = List.copyOf(dates);
        weekdays = weekdays.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(weekdays));
        times = times.stream().sorted().distinct().toList();
        if (times.isEmpty()) {
            throw new IllegalArgumentException("Mindestens eine Uhrzeit nötig");
        }
    }

    public boolean matchesDate(LocalDate date) {
        if (!weekdays.isEmpty() && !weekdays.contains(date.getDayOfWeek())) {
            return false;
        }
        if (dates.isEmpty()) {
            return true;
        }
        for (DateRule rule : dates) {
            if (rule.matches(date)) {
                return true;
            }
        }
        return false;
    }

    /** Alle Startzeitpunkte an den Tagen {@code from} bis {@code to} (inklusive), aufsteigend. */
    public List<Instant> startsBetween(LocalDate from, LocalDate to, ZoneId zone) {
        List<Instant> out = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (matchesDate(date)) {
                for (LocalTime time : times) {
                    out.add(ZonedDateTime.of(date, time, zone).toInstant());
                }
            }
        }
        return out;
    }
}
