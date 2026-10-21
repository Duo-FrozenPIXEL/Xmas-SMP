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

class EventServiceTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    @TempDir
    Path dir;

    private final RecordingHost host = new RecordingHost();
    private final MutableClock clock = new MutableClock(berlin("2026-12-24T17:00:00"));

    private static Instant berlin(String local) {
        return LocalDateTime.parse(local).atZone(BERLIN).toInstant();
    }

    private EventService service() {
        return new EventService(dir, dir, host, EventActionRegistry.withDefaults(), new EventClock(clock),
                XmasLogger.NOOP);
    }

    @Test
    void templateIsWrittenAndParsesWithoutErrors() {
        List<String> messages = service().reload(BERLIN, true);
        assertTrue(Files.exists(dir.resolve(EventService.FILE_NAME)));
        assertEquals("events.jsonc mit Beispiel-Events angelegt", messages.get(0));
        assertEquals("4 Event(s) geladen, Zeitzone Europe/Berlin", messages.get(messages.size() - 1));
        assertEquals(2, messages.size(), "keine Fehler: " + messages);
    }

    @Test
    void templateEventsAreDisabledPlaceholders() {
        EventService service = service();
        service.reload(BERLIN, true);
        assertTrue(service.scheduler().definitions().stream().noneMatch(EventDefinition::enabled));
    }

    @Test
    void brokenFileKeepsPreviousEvents() throws IOException {
        EventService service = service();
        service.reload(BERLIN, true);
        Files.writeString(dir.resolve(EventService.FILE_NAME), "{ \"events\": [ { \"id\": \"x\" } ] }");
        List<String> messages = service.reload(BERLIN, true);
        assertTrue(messages.get(messages.size() - 1).contains("nicht übernommen"), messages.toString());
        assertEquals(4, service.scheduler().definitions().size());
    }

    @Test
    void invalidEventIsSkippedOthersLoad() throws IOException {
        Files.writeString(dir.resolve(EventService.FILE_NAME), """
                { "events": [
                  { "id": "ok", "times": ["18:00"] },
                  { "id": "bad", "times": ["25:00"] },
                  { "id": "bad_action", "times": ["18:00"], "onStart": [ { "type": "fireworks" } ] }
                ] }
                """);
        EventService service = service();
        List<String> messages = service.reload(BERLIN, true);
        assertEquals(1, service.scheduler().definitions().size());
        assertTrue(messages.stream().anyMatch(m -> m.contains("bad") && m.contains("25:00")), messages.toString());
        assertTrue(messages.stream().anyMatch(m -> m.contains("fireworks")), messages.toString());
    }

    @Test
    void stateIsPersistedAcrossInstances() throws IOException {
        writeEve();
        EventService first = service();
        first.reload(BERLIN, true);
        first.tick();
        assertTrue(first.start("eve"));
        first.shutdown();

        EventService second = service();
        second.reload(BERLIN, true);
        assertTrue(second.scheduler().running("eve").isPresent());
    }

    @Test
    void dryRunSimulationOnlyLogsAndRestoresRealState() throws IOException {
        writeEve();
        EventService service = service();
        service.reload(BERLIN, true);
        service.tick();

        service.simulate(berlin("2026-12-24T17:59:58"), true);
        for (int i = 0; i < 5; i++) {
            clock.advance(Duration.ofSeconds(1));
            service.tick();
        }
        assertTrue(service.scheduler().running("eve").isPresent(), "läuft in der Simulation");
        assertTrue(host.log.isEmpty(), "Trockenlauf führt nichts aus: " + host.log);

        assertTrue(service.stopSimulation());
        assertFalse(service.isSimulating());
        assertTrue(service.scheduler().running().isEmpty(), "echter Zustand wiederhergestellt");
        assertFalse(service.stopSimulation());
    }

    @Test
    void liveSimulationExecutesActions() throws IOException {
        writeEve();
        EventService service = service();
        service.reload(BERLIN, true);
        service.simulate(berlin("2026-12-24T17:59:59"), false);
        clock.advance(Duration.ofSeconds(1));
        service.tick();
        assertEquals(List.of("broadcast:start"), host.log);
    }

    private void writeEve() throws IOException {
        Files.writeString(dir.resolve(EventService.FILE_NAME), """
                { "events": [ { "id": "eve", "dates": ["12-24"], "times": ["18:00"], "durationMinutes": 30,
                  "onStart": [ { "type": "broadcast", "message": "start" } ] } ] }
                """);
    }
}
