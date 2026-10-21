package de.xmassmp.event;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventScheduleTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    @Test
    void fixedRangeIsInclusive() throws EventConfigException {
        DateRule rule = DateRule.parse("2026-12-24", "2026-12-26");
        assertInstanceOf(DateRule.Fixed.class, rule);
        assertFalse(rule.matches(LocalDate.of(2026, 12, 23)));
        assertTrue(rule.matches(LocalDate.of(2026, 12, 24)));
        assertTrue(rule.matches(LocalDate.of(2026, 12, 26)));
        assertFalse(rule.matches(LocalDate.of(2026, 12, 27)));
        assertFalse(rule.matches(LocalDate.of(2027, 12, 24)));
    }

    @Test
    void yearlyRangeRepeatsEveryYear() throws EventConfigException {
        DateRule rule = DateRule.parse("12-24", "12-26");
        assertTrue(rule.matches(LocalDate.of(2026, 12, 25)));
        assertTrue(rule.matches(LocalDate.of(2031, 12, 25)));
        assertFalse(rule.matches(LocalDate.of(2026, 12, 27)));
    }

    @Test
    void yearlyRangeCanWrapAroundNewYear() throws EventConfigException {
        DateRule rule = DateRule.parse("12-30", "01-02");
        assertTrue(rule.matches(LocalDate.of(2026, 12, 30)));
        assertTrue(rule.matches(LocalDate.of(2026, 12, 31)));
        assertTrue(rule.matches(LocalDate.of(2027, 1, 1)));
        assertTrue(rule.matches(LocalDate.of(2027, 1, 2)));
        assertFalse(rule.matches(LocalDate.of(2027, 1, 3)));
        assertFalse(rule.matches(LocalDate.of(2026, 12, 29)));
    }

    @Test
    void leapDayOnlyMatchesInLeapYears() throws EventConfigException {
        DateRule rule = DateRule.parse("02-29", "02-29");
        assertTrue(rule.matches(LocalDate.of(2028, 2, 29)));
        assertFalse(rule.matches(LocalDate.of(2027, 2, 28)));
        assertFalse(rule.matches(LocalDate.of(2027, 3, 1)));
    }

    @Test
    void invalidRangesAreRejected() {
        assertThrows(EventConfigException.class, () -> DateRule.parse("2026-12-26", "2026-12-24"));
        assertThrows(EventConfigException.class, () -> DateRule.parse("12-24", "2026-12-26"));
        assertThrows(EventConfigException.class, () -> DateRule.parse("24.12.", "24.12."));
        assertThrows(EventConfigException.class, () -> DateRule.parse("13-01", "13-01"));
    }

    @Test
    void startsUseConfiguredZone() throws EventConfigException {
        EventSchedule schedule = new EventSchedule(List.of(DateRule.parse("12-24", "12-24")), Set.of(),
                List.of(LocalTime.of(18, 0)));
        List<Instant> starts = schedule.startsBetween(LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 28), BERLIN);
        // Winterzeit: 18:00 Berlin = 17:00 UTC
        assertEquals(List.of(Instant.parse("2026-12-24T17:00:00Z")), starts);
        List<Instant> tokyo = schedule.startsBetween(LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 24),
                ZoneId.of("Asia/Tokyo"));
        assertEquals(List.of(Instant.parse("2026-12-24T09:00:00Z")), tokyo);
    }

    @Test
    void summerTimeGapShiftsStartForward() {
        // 29.03.2026: 02:00 → 03:00 in Berlin; 02:30 existiert nicht.
        EventSchedule schedule = new EventSchedule(List.of(), Set.of(), List.of(LocalTime.of(2, 30)));
        LocalDate day = LocalDate.of(2026, 3, 29);
        List<Instant> starts = schedule.startsBetween(day, day, BERLIN);
        assertEquals(1, starts.size());
        assertEquals(ZonedDateTime.of(day, LocalTime.of(3, 30), BERLIN).toInstant(), starts.get(0));
    }

    @Test
    void autumnOverlapStartsOnlyOnce() {
        // 25.10.2026: 02:00–03:00 gibt es zweimal; Start nur bei der ersten (CEST).
        EventSchedule schedule = new EventSchedule(List.of(), Set.of(), List.of(LocalTime.of(2, 30)));
        LocalDate day = LocalDate.of(2026, 10, 25);
        List<Instant> starts = schedule.startsBetween(day, day, BERLIN);
        assertEquals(List.of(Instant.parse("2026-10-25T00:30:00Z")), starts);
    }

    @Test
    void weekdayFilterAndMultipleTimes() {
        EventSchedule schedule = new EventSchedule(List.of(), Set.of(DayOfWeek.SATURDAY),
                List.of(LocalTime.of(20, 0), LocalTime.of(12, 0)));
        // KW 52/2026: Samstag ist der 26.12.
        List<Instant> starts = schedule.startsBetween(LocalDate.of(2026, 12, 21), LocalDate.of(2026, 12, 27), BERLIN);
        assertEquals(List.of(Instant.parse("2026-12-26T11:00:00Z"), Instant.parse("2026-12-26T19:00:00Z")), starts);
    }
}
