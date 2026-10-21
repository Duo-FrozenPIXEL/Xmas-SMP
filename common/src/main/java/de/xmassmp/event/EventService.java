package de.xmassmp.event;

import de.xmassmp.event.action.EventActionRegistry;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.util.JsonFiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bündelt Scheduler, {@code events.jsonc}, Zustandsspeicher und Testmodus.
 *
 * <p>Testmodus ({@link #simulate}): Der echte Zustand wird gesichert, die Uhr springt auf das
 * simulierte Datum und läuft von dort normal weiter. Mit {@code dryRun} werden Aktionen nur
 * geloggt. {@link #stopSimulation()} stellt den gesicherten Zustand wieder her; während der
 * Simulation wird nichts gespeichert.
 */
public final class EventService {

    public static final String FILE_NAME = "events.jsonc";
    static final String TEMPLATE = "/xmassmp/events.default.jsonc";
    private static final Duration SAVE_INTERVAL = Duration.ofSeconds(60);

    private final Path file;
    private final EventHost host;
    private final XmasLogger logger;
    private final EventClock clock;
    private final EventLoader loader;
    private final EventStateStore store;
    private final EventScheduler scheduler;

    private EventState realState;
    private boolean dryRun;
    private Instant lastSave = Instant.MIN;

    public EventService(Path configDirectory, Path dataDirectory, EventHost host, EventActionRegistry actions,
                        EventClock clock, XmasLogger logger) {
        this.file = configDirectory.resolve(FILE_NAME);
        this.host = host;
        this.logger = logger;
        this.clock = clock;
        this.loader = new EventLoader(actions);
        this.store = new EventStateStore(dataDirectory, logger);
        this.scheduler = new EventScheduler(host, logger);
        this.scheduler.restore(store.load());
    }

    public EventScheduler scheduler() {
        return scheduler;
    }

    public EventClock clock() {
        return clock;
    }

    /** Lädt {@code events.jsonc}; legt die Beispiel-Datei an, falls sie fehlt. Liefert Meldungen. */
    public List<String> reload(ZoneId zone, boolean catchUpMissed) {
        List<String> messages = new ArrayList<>();
        try {
            if (Files.notExists(file)) {
                try (InputStream in = EventService.class.getResourceAsStream(TEMPLATE)) {
                    if (in == null) {
                        throw new IOException("Vorlage " + TEMPLATE + " fehlt im JAR");
                    }
                    JsonFiles.writeAtomic(file, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
                messages.add(FILE_NAME + " mit Beispiel-Events angelegt");
            }
            EventLoader.Result result = loader.parse(Files.readString(file, StandardCharsets.UTF_8));
            if (result.events().isEmpty() && !result.errors().isEmpty()) {
                messages.addAll(result.errors());
                messages.add(FILE_NAME + " nicht übernommen – vorherige Events bleiben aktiv");
                return messages;
            }
            scheduler.configure(result.events(), result.messages(), zone, catchUpMissed);
            messages.addAll(result.errors());
            messages.add(result.events().size() + " Event(s) geladen, Zeitzone " + zone);
        } catch (IOException e) {
            messages.add(FILE_NAME + " konnte nicht gelesen werden: " + e.getMessage());
        }
        return messages;
    }

    /** Einmal pro Sekunde aufrufen. */
    public void tick() {
        scheduler.tick(clock.now());
        if (isSimulating()) {
            scheduler.consumeChanged();
            return;
        }
        Instant real = clock.realNow();
        if (scheduler.consumeChanged() || Duration.between(lastSave, real).compareTo(SAVE_INTERVAL) >= 0) {
            save();
        }
    }

    public boolean start(String id) {
        return scheduler.startNow(id, clock.now());
    }

    public boolean stop(String id) {
        return scheduler.stopNow(id);
    }

    public Optional<Instant> nextStart(String id) {
        return scheduler.nextStart(id, clock.now());
    }

    public double modifier(String key) {
        return scheduler.modifier(key);
    }

    public void simulate(Instant target, boolean dry) {
        if (!isSimulating()) {
            realState = scheduler.snapshot();
            save();
        }
        // Simulation startet ohne laufende Events; was zum Zeitpunkt läuft, holt der Catch-up nach.
        scheduler.restore(new EventState(null, List.of()));
        dryRun = dry;
        scheduler.setHost(dry ? new DryRunHost(logger) : host);
        clock.simulate(target);
        logger.info("Event-Testmodus: simulierte Zeit " + target + (dry ? " (Trockenlauf)" : ""));
        scheduler.tick(clock.now());
    }

    public boolean stopSimulation() {
        if (!isSimulating()) {
            return false;
        }
        clock.reset();
        scheduler.setHost(host);
        scheduler.restore(realState);
        realState = null;
        dryRun = false;
        logger.info("Event-Testmodus beendet");
        return true;
    }

    public boolean isSimulating() {
        return clock.isSimulated();
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void shutdown() {
        stopSimulation();
        save();
    }

    private void save() {
        store.save(scheduler.snapshot());
        lastSave = clock.realNow();
    }

    /** Führt nichts aus, sondern protokolliert nur. */
    private record DryRunHost(XmasLogger logger) implements EventHost {
        @Override
        public void broadcast(String legacyText) {
            logger.info("[Trockenlauf] Broadcast: " + legacyText);
        }

        @Override
        public void title(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
            logger.info("[Trockenlauf] Titel: " + title + " | " + subtitle);
        }

        @Override
        public void playSound(String sound, float volume, float pitch) {
            logger.info("[Trockenlauf] Sound: " + sound);
        }

        @Override
        public void runCommand(String command) {
            logger.info("[Trockenlauf] Befehl: /" + command);
        }
    }
}
