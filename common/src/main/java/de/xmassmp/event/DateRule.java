package de.xmassmp.event;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeParseException;

/**
 * Datumsbereich eines Events, beide Enden inklusive.
 *
 * <ul>
 *   <li>{@code 2026-12-24} – festes Datum (einmalig)</li>
 *   <li>{@code 12-24} – jedes Jahr; Bereiche dürfen über den Jahreswechsel gehen
 *       ({@code 12-31} bis {@code 01-01})</li>
 * </ul>
 */
public sealed interface DateRule {

    boolean matches(LocalDate date);

    static DateRule parse(String from, String to) throws EventConfigException {
        boolean fromYearly = isMonthDay(from);
        boolean toYearly = isMonthDay(to);
        if (fromYearly != toYearly) {
            throw new EventConfigException("Datumsbereich mischt jährlich (MM-dd) und fest (yyyy-MM-dd): "
                    + from + " bis " + to);
        }
        try {
            if (fromYearly) {
                return new Yearly(MonthDay.parse("--" + from), MonthDay.parse("--" + to));
            }
            LocalDate start = LocalDate.parse(from);
            LocalDate end = LocalDate.parse(to);
            if (end.isBefore(start)) {
                throw new EventConfigException("Enddatum " + to + " liegt vor " + from);
            }
            return new Fixed(start, end);
        } catch (DateTimeParseException e) {
            throw new EventConfigException("Ungültiges Datum '" + from + "'/'" + to
                    + "' – erwartet yyyy-MM-dd oder MM-dd");
        }
    }

    private static boolean isMonthDay(String text) {
        return text.length() == 5 && text.charAt(2) == '-';
    }

    record Fixed(LocalDate from, LocalDate to) implements DateRule {
        @Override
        public boolean matches(LocalDate date) {
            return !date.isBefore(from) && !date.isAfter(to);
        }
    }

    /** Jährlich wiederkehrend. Ist {@code to} vor {@code from}, läuft der Bereich über Silvester. */
    record Yearly(MonthDay from, MonthDay to) implements DateRule {
        @Override
        public boolean matches(LocalDate date) {
            MonthDay day = MonthDay.from(date);
            if (!to.isBefore(from)) {
                return !day.isBefore(from) && !day.isAfter(to);
            }
            return !day.isBefore(from) || !day.isAfter(to);
        }
    }
}
