package de.xmassmp.event;

import de.xmassmp.event.action.EventActionRegistry;
import de.xmassmp.platform.XmasLogger;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventSchedulerTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private final RecordingHost host = new RecordingHost();

    private EventScheduler scheduler(boolean catchUp, String events) {
        EventLoader.Result result = new EventLoader(EventActionRegistry.withDefaults())
                .parse("{ \"events\": [" + events + "] }");
        assertEquals(List.of(), result.errors());
        EventScheduler scheduler = new EventScheduler(host, XmasLogger.NOOP);
        scheduler.configure(result.events(), result.messages(), BERLIN, catchUp);
        return scheduler;
    }

    private static Instant berlin(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(BERLIN).toInstant();
    }

    /** Tickt jede Sekunde von {@code from} bis {@code to} (inklusive). */
    private static void runSeconds(EventScheduler scheduler, String from, String to) {
        Instant end = berlin(to);
        for (Instant t = berlin(from); !t.isAfter(end); t = t.plusSeconds(1)) {
            scheduler.tick(t);
        }
    }

    private static final String EVE = """
            { "id": "eve", "name": "Heiligabend", "dates": ["12-24"], "times": ["18:00"],
              "durationMinutes": 30, "announceMinutes": [10, 1], "countdownSeconds": 3,
              "onStart": [ { "type": "broadcast", "message": "start {name}" },
                           { "type": "command", "command": "/say {id}" } ],
              "onEnd": [ { "type": "broadcast", "message": "end {name}" } ],
              "modifiers": { "cooldown_multiplier": 0.5 } }
            """;

    @Test
    void announcementsCountdownStartAndEndRunInOrder() {
        EventScheduler s = scheduler(true, EVE);
        runSeconds(s, "2026-12-24T17:49:00", "2026-12-24T18:31:00");

        assertEquals(List.of(
                "broadcast:§6✦ §eHeiligabend §6beginnt in §e10 §6Minute(n)!",
                "broadcast:§6✦ §eHeiligabend §6beginnt in §e1 §6Minute(n)!",
                "title:§c3|§eHeiligabend",
                "title:§c2|§eHeiligabend",
                "title:§c1|§eHeiligabend",
                "broadcast:start Heiligabend",
                "command:say eve",
                "broadcast:end Heiligabend"), host.without("sound:"));
        assertTrue(s.running().isEmpty());
    }

    @Test
    void modifiersApplyOnlyWhileRunning() {
        EventScheduler s = scheduler(true, EVE);
        runSeconds(s, "2026-12-24T17:59:58", "2026-12-24T18:00:00");
        assertEquals(0.5, s.modifier("cooldown_multiplier"));
        assertEquals(1.0, s.modifier("energy_multiplier"));
        runSeconds(s, "2026-12-24T18:29:58", "2026-12-24T18:30:00");
        assertEquals(1.0, s.modifier("cooldown_multiplier"));
    }

    @Test
    void countdownCrossesYearBoundary() {
        EventScheduler s = scheduler(true, """
                { "id": "new_year", "dates": ["01-01"], "times": ["00:00"], "durationMinutes": 0,
                  "countdownSeconds": 10, "onStart": [ { "type": "broadcast", "message": "prost" } ] }
                """);
        runSeconds(s, "2026-12-31T23:59:45", "2027-01-01T00:00:05");
        List<String> lines = host.without("sound:");
        assertEquals(11, lines.size());
        assertEquals("title:§c10|§enew_year", lines.get(0));
        assertEquals("title:§c1|§enew_year", lines.get(9));
        assertEquals("broadcast:prost", lines.get(10));
        assertTrue(s.running().isEmpty(), "punktuelles Event läuft nicht weiter");
    }

    @Test
    void eventRunsAcrossMidnight() {
        EventScheduler s = scheduler(true, """
                { "id": "party", "dates": ["2026-12-31"], "times": ["23:30"], "durationMinutes": 60,
                  "onEnd": [ { "type": "broadcast", "message": "vorbei" } ] }
                """);
        runSeconds(s, "2026-12-31T23:29:59", "2026-12-31T23:30:01");
        assertTrue(s.running("party").isPresent());
        runSeconds(s, "2027-01-01T00:29:58", "2027-01-01T00:30:00");
        assertFalse(s.running("party").isPresent());
        assertEquals(List.of("broadcast:vorbei"), host.log);
        // Festes Datum: im nächsten Jahr kein Termin mehr.
        assertTrue(s.nextStart("party", berlin("2027-01-02T00:00:00")).isEmpty());
    }

    @Test
    void backToBackOccurrencesEndBeforeNextStart() {
        EventScheduler s = scheduler(true, """
                { "id": "round", "dates": ["12-24"], "times": ["18:00", "18:30"], "durationMinutes": 30,
                  "onStart": [ { "type": "broadcast", "message": "start" } ],
                  "onEnd": [ { "type": "broadcast", "message": "end" } ] }
                """);
        runSeconds(s, "2026-12-24T17:59:59", "2026-12-24T18:00:00");
        runSeconds(s, "2026-12-24T18:29:59", "2026-12-24T18:30:00");
        runSeconds(s, "2026-12-24T18:59:59", "2026-12-24T19:00:00");
        assertEquals(List.of("broadcast:start", "broadcast:end", "broadcast:start", "broadcast:end"), host.log);
    }

    @Test
    void missedStartIsCaughtUpAfterDowntime() {
        EventScheduler s = scheduler(true, EVE);
        s.tick(berlin("2026-12-24T17:55:00"));
        // Server war 25 Minuten aus.
        s.tick(berlin("2026-12-24T18:20:00"));
        assertEquals(List.of("broadcast:start Heiligabend", "command:say eve"), host.without("sound:"));
        assertEquals(berlin("2026-12-24T18:30:00"), s.running("eve").orElseThrow().end());
    }

    @Test
    void missedStartIsSkippedWithoutCatchUp() {
        EventScheduler s = scheduler(false, EVE);
        s.tick(berlin("2026-12-24T17:55:00"));
        s.tick(berlin("2026-12-24T18:20:00"));
        assertTrue(host.log.isEmpty());
        assertTrue(s.running().isEmpty());
    }

    @Test
    void firstStartInsideEventWindowCatchesUp() {
        EventScheduler s = scheduler(true, EVE);
        s.tick(berlin("2026-12-24T18:10:00"));
        assertTrue(s.running("eve").isPresent());
    }

    @Test
    void eventEndingDuringDowntimeEndsSilently() {
        EventScheduler s = scheduler(true, EVE);
        runSeconds(s, "2026-12-24T17:59:59", "2026-12-24T18:00:00");
        host.log.clear();
        s.tick(berlin("2026-12-24T19:00:00"));
        assertTrue(s.running().isEmpty());
        assertTrue(host.log.isEmpty(), "keine End-Aktionen nach Ausfall: " + host.log);
    }

    @Test
    void runningEventSurvivesRestart() {
        EventScheduler before = scheduler(true, EVE);
        runSeconds(before, "2026-12-24T17:59:59", "2026-12-24T18:00:00");
        EventState state = before.snapshot();

        host.log.clear();
        EventScheduler after = scheduler(true, EVE);
        after.restore(state);
        // Neustart nach 20 s – innerhalb der Toleranz, normaler Betrieb.
        runSeconds(after, "2026-12-24T18:00:20", "2026-12-24T18:30:00");
        assertEquals(List.of("broadcast:end Heiligabend"), host.log, "kein zweiter Start, normales Ende");
    }

    @Test
    void longRestartDoesNotStartRunningEventTwice() {
        EventScheduler before = scheduler(true, EVE);
        runSeconds(before, "2026-12-24T17:59:59", "2026-12-24T18:00:00");
        EventState state = before.snapshot();

        host.log.clear();
        EventScheduler after = scheduler(true, EVE);
        after.restore(state);
        after.tick(berlin("2026-12-24T18:10:00"));
        assertTrue(host.log.isEmpty());
        assertTrue(after.running("eve").isPresent());
    }

    @Test
    void manualStartAndStopRunActionsAndIgnoreEnabledFlag() {
        EventScheduler s = scheduler(true, EVE.replace("\"name\"", "\"enabled\": false, \"name\""));
        runSeconds(s, "2026-12-24T17:49:00", "2026-12-24T18:01:00");
        assertTrue(host.log.isEmpty(), "deaktiviertes Event startet nicht automatisch");

        Instant now = berlin("2026-12-24T18:05:00");
        assertTrue(s.startNow("eve", now));
        assertFalse(s.startNow("eve", now), "läuft schon");
        assertEquals(0.5, s.modifier("cooldown_multiplier"));
        assertTrue(s.stopNow("eve"));
        assertFalse(s.stopNow("eve"));
        assertEquals(List.of("broadcast:start Heiligabend", "command:say eve", "broadcast:end Heiligabend"), host.log);
    }

    @Test
    void manualRunEndsAfterDuration() {
        EventScheduler s = scheduler(true, EVE);
        Instant now = berlin("2026-06-01T12:00:00");
        s.tick(now);
        s.startNow("eve", now);
        runSeconds(s, "2026-06-01T12:00:01", "2026-06-01T12:30:00");
        assertTrue(s.running().isEmpty());
    }

    @Test
    void removingRunningEventFromConfigEndsIt() {
        EventScheduler s = scheduler(true, EVE);
        s.startNow("eve", berlin("2026-06-01T12:00:00"));
        s.configure(List.of(), EventMessages.DEFAULTS, BERLIN, true);
        assertTrue(s.running().isEmpty());
    }

    @Test
    void nextStartFindsNextYearlyOccurrence() {
        EventScheduler s = scheduler(true, EVE);
        assertEquals(berlin("2026-12-24T18:00:00"), s.nextStart("eve", berlin("2026-06-01T00:00:00")).orElseThrow());
        assertEquals(berlin("2027-12-24T18:00:00"), s.nextStart("eve", berlin("2026-12-24T18:00:00")).orElseThrow());
    }
}
