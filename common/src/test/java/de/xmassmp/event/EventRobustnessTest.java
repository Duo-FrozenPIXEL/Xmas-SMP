package de.xmassmp.event;

import de.xmassmp.MutableClock;
import de.xmassmp.event.action.EventActionRegistry;
import de.xmassmp.platform.XmasLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testmodus, Pausen, Zeitsprünge und Platzhalter (bug.md Nr. 3–6, 15–19). */
class EventRobustnessTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private static final String EVE = """
            { "id": "eve", "name": "Heiligabend", "dates": ["12-24"], "times": ["18:00"],
              "durationMinutes": 30, "announceMinutes": [10],
              "onStart": [ { "type": "broadcast", "message": "start" } ],
              "onEnd": [ { "type": "broadcast", "message": "end" } ],
              "modifiers": { "cooldown_multiplier": 0.5 } }
            """;

    @TempDir
    Path dir;

    private final RecordingHost host = new RecordingHost();
    private final MutableClock clock = new MutableClock(berlin("2026-12-24T17:00:00"));

    private static Instant berlin(String local) {
        return LocalDateTime.parse(local).atZone(BERLIN).toInstant();
    }

    private EventScheduler scheduler(String events) {
        EventLoader.Result result = new EventLoader(EventActionRegistry.withDefaults())
                .parse("{ \"events\": [" + events + "] }");
        assertEquals(List.of(), result.errors());
        EventScheduler scheduler = new EventScheduler(host, XmasLogger.NOOP);
        scheduler.configure(result.events(), result.messages(), BERLIN, true);
        return scheduler;
    }

    private static void runSeconds(EventScheduler scheduler, String from, String to) {
        Instant end = berlin(to);
        for (Instant t = berlin(from); !t.isAfter(end); t = t.plusSeconds(1)) {
            scheduler.tick(t);
        }
    }

    private EventService service() throws IOException {
        Files.writeString(dir.resolve(EventService.FILE_NAME), "{ \"events\": [" + EVE + "] }");
        EventService service = new EventService(dir, dir, host, EventActionRegistry.withDefaults(),
                new EventClock(clock), XmasLogger.NOOP);
        service.reload(BERLIN, true);
        service.tick();
        return service;
    }

    private void tickSeconds(EventService service, int seconds) {
        for (int i = 0; i < seconds; i++) {
            clock.advance(Duration.ofSeconds(1));
            service.tick();
        }
    }

    private long count(String line) {
        return host.log.stream().filter(line::equals).count();
    }

    // ------------------------------------------------------------------ Scheduler

    @Test
    void noAnnouncementWhileEventIsStillRunning() {
        EventScheduler s = scheduler(EVE);
        s.tick(berlin("2026-12-24T17:40:00"));
        assertTrue(s.startNow("eve", berlin("2026-12-24T17:40:00")));
        runSeconds(s, "2026-12-24T17:40:01", "2026-12-24T17:55:00");
        assertEquals(List.of("broadcast:start"), host.without("sound:"), "keine Ankündigung, Start folgt nicht");
    }

    @Test
    void clockSetBackDoesNotRepeatEvents() {
        EventScheduler s = scheduler(EVE);
        runSeconds(s, "2026-12-24T17:59:58", "2026-12-24T18:31:00");
        assertEquals(1, count("broadcast:start"));
        assertEquals(1, count("broadcast:end"));

        // Uhr 25 Minuten zurück: Der Plan wartet, statt das Event nachzuholen.
        runSeconds(s, "2026-12-24T18:06:00", "2026-12-24T18:31:30");
        assertEquals(1, count("broadcast:start"), host.log.toString());
        assertTrue(s.running().isEmpty());
    }

    @Test
    void largeBackwardJumpRestartsPlanWithoutCatchUp() {
        EventScheduler s = scheduler(EVE);
        s.tick(berlin("2026-12-26T12:00:00"));
        s.tick(berlin("2026-12-24T18:05:00"));
        s.tick(berlin("2026-12-24T18:05:01"));
        assertTrue(s.running().isEmpty(), "kein Nachholen nach großem Rücksprung");
        assertEquals(0, count("broadcast:start"));
    }

    @Test
    void resumeAfterPauseEndsWithEndActions() {
        EventScheduler s = scheduler(EVE);
        runSeconds(s, "2026-12-24T17:59:59", "2026-12-24T18:00:01");
        assertTrue(s.running("eve").isPresent());
        s.resume(berlin("2026-12-24T18:45:00"));
        assertTrue(s.running().isEmpty());
        assertEquals(1, count("broadcast:end"), "End-Aktion trotz Pause");
    }

    @Test
    void serverDowntimeStillEndsSilently() {
        EventScheduler s = scheduler(EVE);
        runSeconds(s, "2026-12-24T17:59:59", "2026-12-24T18:00:01");
        s.tick(berlin("2026-12-24T18:45:00"));
        assertTrue(s.running().isEmpty());
        assertEquals(0, count("broadcast:end"));
    }

    @Test
    void nextStartFindsLeapDay() {
        EventScheduler s = scheduler("""
                { "id": "leap", "dates": ["02-29"], "times": ["12:00"] }
                """);
        assertEquals(berlin("2028-02-29T12:00:00"), s.nextStart("leap", berlin("2026-03-01T00:00:00")).orElseThrow());
    }

    @Test
    void placeholdersAreReplacedOnceAndKeepFractions() {
        EventScheduler s = scheduler("""
                { "id": "bonus", "name": "Bonus {id}", "dates": ["12-24"], "times": ["18:00"],
                  "durationMinutes": 0.5, "onStart": [ { "type": "broadcast", "message": "{name}: {duration} min {x}" } ] }
                """);
        runSeconds(s, "2026-12-24T17:59:59", "2026-12-24T18:00:00");
        assertEquals(List.of("broadcast:Bonus {id}: 0.5 min {x}"), host.without("sound:"));
    }

    @Test
    void minutesFormat() {
        assertEquals("30", EventScheduler.minutes(Duration.ofMinutes(30)));
        assertEquals("0.5", EventScheduler.minutes(Duration.ofSeconds(30)));
        assertEquals("1.25", EventScheduler.minutes(Duration.ofSeconds(75)));
    }

    // ------------------------------------------------------------------ Testmodus

    @Test
    void stopSimulationRunsEndActionsOfSimulatedEvents() throws IOException {
        EventService service = service();
        service.simulate(berlin("2026-12-24T17:59:59"), false);
        tickSeconds(service, 2);
        assertEquals(1, count("broadcast:start"));

        assertTrue(service.stopSimulation());
        assertEquals(1, count("broadcast:end"), "simuliertes Event endet mit End-Aktionen");
        assertTrue(service.scheduler().running().isEmpty());
    }

    @Test
    void realScheduleKeepsRunningDuringSimulation() throws IOException {
        clock.set(berlin("2026-12-24T17:59:55"));
        EventService service = service();
        service.simulate(berlin("2026-07-01T12:00:00"), true);
        tickSeconds(service, 10);
        assertEquals(1, count("broadcast:start"), "echtes Event startet trotz Testmodus");

        service.stopSimulation();
        assertTrue(service.scheduler().running("eve").isPresent());
        tickSeconds(service, 30 * 60);
        assertEquals(1, count("broadcast:end"), "und endet regulär mit End-Aktionen");
    }

    @Test
    void dryRunModifiersDoNotTouchRealGame() throws IOException {
        EventService service = service();
        service.simulate(berlin("2026-12-24T17:59:59"), true);
        tickSeconds(service, 2);
        assertTrue(service.scheduler().running("eve").isPresent(), "läuft in der Simulation");
        assertEquals(1.0, service.modifier("cooldown_multiplier"));

        service.simulate(berlin("2026-12-24T17:59:59"), false);
        tickSeconds(service, 2);
        assertEquals(0.5, service.modifier("cooldown_multiplier"), "ohne Trockenlauf wirkt die Simulation");
    }

    @Test
    void realModifiersStayActiveDuringSimulation() throws IOException {
        clock.set(berlin("2026-12-24T17:59:59"));
        EventService service = service();
        tickSeconds(service, 2);
        assertEquals(0.5, service.modifier("cooldown_multiplier"));
        service.simulate(berlin("2026-07-01T12:00:00"), true);
        tickSeconds(service, 1);
        assertEquals(0.5, service.modifier("cooldown_multiplier"));
    }

    // ------------------------------------------------------------------ Pausen

    @Test
    void pausedPluginEndsExpiredEventsWithEndActions() throws IOException {
        clock.set(berlin("2026-12-24T17:59:59"));
        EventService service = service();
        tickSeconds(service, 2);
        assertTrue(service.scheduler().running("eve").isPresent());

        service.pause();
        clock.advance(Duration.ofMinutes(45));
        service.tick();
        assertTrue(service.scheduler().running().isEmpty());
        assertEquals(1, count("broadcast:end"));
    }

    @Test
    void brokenFileAtStartupKeepsRestoredEvents() throws IOException {
        clock.set(berlin("2026-12-24T17:59:59"));
        EventService first = service();
        tickSeconds(first, 2);
        first.shutdown();

        Files.writeString(dir.resolve(EventService.FILE_NAME), "{ \"events\": [ { \"id\": \"x\" } ] }");
        EventService second = new EventService(dir, dir, host, EventActionRegistry.withDefaults(),
                new EventClock(clock), XmasLogger.NOOP);
        List<String> messages = second.reload(BERLIN, true);
        assertTrue(messages.get(messages.size() - 1).contains("keine Events aktiv"), messages.toString());
        tickSeconds(second, 5);
        assertTrue(second.scheduler().running("eve").isPresent(), "wiederhergestelltes Event bleibt erhalten");
        assertFalse(second.reloadResult(BERLIN, true).success());

        Files.writeString(dir.resolve(EventService.FILE_NAME), "{ \"events\": [" + EVE + "] }");
        assertTrue(second.reloadResult(BERLIN, true).success());
        clock.advance(Duration.ofMinutes(40));
        second.tick();
        assertTrue(second.scheduler().running().isEmpty());
        assertEquals(1, count("broadcast:end"), "endet nach der Reparatur mit End-Aktionen");
    }

    @Test
    void enabledFlagIsWrittenWithoutLosingComments() throws IOException {
        Path file = dir.resolve(EventService.FILE_NAME);
        Files.writeString(file, "// Kommentar bleibt\n{ \"events\": [\n  // Heiligabend\n  " + EVE + "] }");
        EventService service = new EventService(dir, dir, host, EventActionRegistry.withDefaults(),
                new EventClock(clock), XmasLogger.NOOP);
        service.reload(BERLIN, true);
        assertTrue(service.setEnabled("eve", false).success());
        assertFalse(service.scheduler().definition("eve").orElseThrow().enabled());
        String text = Files.readString(file);
        assertTrue(text.contains("// Kommentar bleibt") && text.contains("// Heiligabend"), text);
        assertFalse(service.setEnabled("unbekannt", true).success());
    }
}
