package de.xmassmp.event;

import de.xmassmp.config.ConfigManager;
import de.xmassmp.config.JsoncEditor;
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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bündelt Scheduler, {@code events.jsonc}, Zustandsspeicher und Testmodus.
 *
 * <p>Testmodus ({@link #simulate}): Ein eigener Scheduler läuft ab dem simulierten Datum mit,
 * der echte Plan läuft in echter Zeit unverändert weiter und wird weiter gespeichert. Mit
 * {@code dryRun} werden Aktionen der Simulation nur geloggt, ihre Modifikatoren wirken nicht und
 * Listener werden nicht benachrichtigt. {@link #stopSimulation()} beendet die simulierten Events
 * mit ihren End-Aktionen, damit z. B. per Befehl geändertes Wetter zurückgesetzt wird.
 *
 * <p>Solange keine gültige {@code events.jsonc} geladen ist oder das Plugin abgeschaltet ist
 * ({@link #pause()}), ruht der echte Plan. Danach setzt er mit {@link EventScheduler#resume} fort.
 */
public final class EventService {

    public static final String FILE_NAME = "events.jsonc";
    static final String TEMPLATE = "/xmassmp/events.default.jsonc";
    private static final Duration SAVE_INTERVAL = Duration.ofSeconds(60);

    private record Config(List<EventDefinition> events, EventMessages messages, ZoneId zone, boolean catchUp) {
    }

    private final Path file;
    private final EventHost host;
    private final XmasLogger logger;
    private final EventClock clock;
    private final EventLoader loader;
    private final EventStateStore store;
    private final EventScheduler scheduler;
    private final List<EventScheduler.Listener> listeners = new CopyOnWriteArrayList<>();

    private Config config;
    private EventScheduler simulation;
    private boolean dryRun;
    private boolean paused;
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

    /** Der gerade aktive Plan: im Testmodus der simulierte, sonst der echte. */
    public EventScheduler scheduler() {
        return simulation != null ? simulation : scheduler;
    }

    public EventClock clock() {
        return clock;
    }

    /** Benachrichtigung über Start und Ende echter Events und nicht-trockener Simulationen. */
    public void addListener(EventScheduler.Listener listener) {
        listeners.add(listener);
        scheduler.addListener(listener);
        if (simulation != null && !dryRun) {
            simulation.addListener(listener);
        }
    }

    /** Lädt {@code events.jsonc}; legt die Beispiel-Datei an, falls sie fehlt. Liefert Meldungen. */
    public List<String> reload(ZoneId zone, boolean catchUpMissed) {
        return reloadResult(zone, catchUpMissed).messages();
    }

    /** Wie {@link #reload}, zusätzlich mit der Angabe, ob die Datei übernommen wurde. */
    public ConfigManager.LoadResult reloadResult(ZoneId zone, boolean catchUpMissed) {
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
                messages.add(notApplied());
                return new ConfigManager.LoadResult(false, messages);
            }
            config = new Config(List.copyOf(result.events()), result.messages(), zone, catchUpMissed);
            apply(scheduler);
            if (simulation != null) {
                apply(simulation);
            }
            messages.addAll(result.errors());
            messages.add(result.events().size() + " Event(s) geladen, Zeitzone " + zone);
            return new ConfigManager.LoadResult(true, messages);
        } catch (IOException e) {
            messages.add(FILE_NAME + " konnte nicht gelesen werden: " + e.getMessage());
            if (config == null) {
                messages.add(notApplied());
            }
            return new ConfigManager.LoadResult(false, messages);
        }
    }

    /** Setzt {@code enabled} eines Events direkt in {@code events.jsonc} (Kommentare bleiben) und lädt neu. */
    public ConfigManager.LoadResult setEnabled(String id, boolean enabled) {
        if (config == null) {
            return new ConfigManager.LoadResult(false, List.of(notApplied()));
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            JsonFiles.writeAtomic(file, JsoncEditor.set(text, List.of("events", "id=" + id, "enabled"),
                    Boolean.toString(enabled)));
        } catch (IOException | IllegalArgumentException e) {
            return new ConfigManager.LoadResult(false,
                    List.of(FILE_NAME + " konnte nicht geändert werden: " + e.getMessage()));
        }
        return reloadResult(config.zone(), config.catchUp());
    }

    private String notApplied() {
        return config == null
                ? FILE_NAME + " nicht übernommen – keine Events aktiv, bis die Datei repariert ist (/xmas reload)"
                : FILE_NAME + " nicht übernommen – vorherige Events bleiben aktiv";
    }

    private void apply(EventScheduler target) {
        target.configure(config.events(), config.messages(), config.zone(), config.catchUp());
    }

    /** Einmal pro Sekunde aufrufen. */
    public void tick() {
        if (config == null) {
            // Ohne Definitionen würden wiederhergestellte Events ohne End-Aktionen enden.
            markPausedIfStarted();
            return;
        }
        if (paused) {
            paused = false;
            scheduler.resume(clock.realNow());
            if (simulation != null) {
                simulation.resume(clock.now());
            }
        } else {
            scheduler.tick(clock.realNow());
            if (simulation != null) {
                simulation.tick(clock.now());
            }
        }
        if (simulation != null) {
            simulation.consumeChanged();
        }
        Instant real = clock.realNow();
        if (scheduler.consumeChanged() || Duration.between(lastSave, real).compareTo(SAVE_INTERVAL) >= 0) {
            save();
        }
    }

    /**
     * Plugin abgeschaltet: Der Plan ruht. Der nächste {@link #tick()} setzt fort und beendet
     * inzwischen abgelaufene Events mit End-Aktionen, statt sie wie nach einem Serverausfall still
     * zu beenden.
     */
    public void pause() {
        markPausedIfStarted();
    }

    private void markPausedIfStarted() {
        if (scheduler.snapshot().lastProcessed() != null) {
            paused = true;
        }
    }

    public boolean start(String id) {
        return scheduler().startNow(id, clock.now());
    }

    public boolean stop(String id) {
        return scheduler().stopNow(id);
    }

    public Optional<Instant> nextStart(String id) {
        return scheduler().nextStart(id, clock.now());
    }

    /** Modifikatoren echter Events, im Testmodus ohne Trockenlauf zusätzlich die simulierten. */
    public double modifier(String key) {
        double value = scheduler.modifier(key);
        if (simulation != null && !dryRun) {
            value *= simulation.modifier(key);
        }
        return value;
    }

    public void simulate(Instant target, boolean dry) {
        if (simulation != null) {
            // Vorherige Simulation sauber beenden, sonst blieben ihre Änderungen bestehen.
            simulation.stopAll();
        } else {
            save();
        }
        dryRun = dry;
        simulation = new EventScheduler(dry ? new DryRunHost(logger) : host, logger);
        if (!dry) {
            listeners.forEach(simulation::addListener);
        }
        if (config != null) {
            apply(simulation);
        }
        clock.simulate(target);
        logger.info("Event-Testmodus: simulierte Zeit " + target + (dry ? " (Trockenlauf)" : ""));
        // Simulation startet ohne laufende Events; was zum Zeitpunkt läuft, holt der Catch-up nach.
        simulation.tick(clock.now());
    }

    public boolean stopSimulation() {
        if (simulation == null) {
            return false;
        }
        simulation.stopAll();
        simulation = null;
        clock.reset();
        dryRun = false;
        logger.info("Event-Testmodus beendet");
        return true;
    }

    public boolean isSimulating() {
        return simulation != null;
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
