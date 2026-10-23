package de.xmassmp.event;

import de.xmassmp.event.action.ActionContext;
import de.xmassmp.event.action.EventAction;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.text.Messages;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Führt Events zu festen Tagen und Uhrzeiten aus. Rein zeitgesteuert über {@link #tick(Instant)} –
 * der Aufrufer bestimmt die Uhr (echte Zeit oder Simulation).
 *
 * <p>Jeder Tick arbeitet alle Auslöser im Fenster (letzter Tick, jetzt] in zeitlicher
 * Reihenfolge ab: Enden, Ankündigungen, Countdown, Starts. Liegt zwischen zwei Ticks mehr als
 * {@link #MAX_GAP} (Server war aus, Zeitsprung), gilt das als Ausfall:
 * <ul>
 *   <li>während der Pause abgelaufene Events enden still (ohne End-Aktionen),</li>
 *   <li>verpasste Ankündigungen entfallen,</li>
 *   <li>mit {@code catchUpMissed} werden Events, deren Start in die Pause fiel und die noch
 *       laufen würden, nachträglich gestartet; sonst übersprungen.</li>
 * </ul>
 */
public final class EventScheduler {

    public static final Duration MAX_GAP = Duration.ofSeconds(30);

    /** Benachrichtigung über Start und Ende, z. B. für den Christmas-Twist. */
    public interface Listener {
        default void started(EventDefinition event, RunningEvent running) {
        }

        default void ended(EventDefinition event, RunningEvent running) {
        }
    }

    private enum Kind { END, ANNOUNCE, COUNTDOWN, START }

    private record Trigger(Instant at, Kind kind, String eventId, Instant occurrenceStart, long secondsLeft) {
    }

    private final XmasLogger logger;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private EventHost host;
    private Map<String, EventDefinition> definitions = Map.of();
    private EventMessages messages = EventMessages.DEFAULTS;
    private ZoneId zone = ZoneId.of("Europe/Berlin");
    private boolean catchUpMissed = true;

    private final Map<String, RunningEvent> running = new LinkedHashMap<>();
    private Instant lastProcessed;
    private boolean changed;

    public EventScheduler(EventHost host, XmasLogger logger) {
        this.host = host;
        this.logger = logger;
    }

    public void configure(Collection<EventDefinition> events, EventMessages newMessages, ZoneId newZone,
                          boolean catchUp) {
        Map<String, EventDefinition> map = new LinkedHashMap<>();
        events.forEach(e -> map.put(e.id(), e));
        this.definitions = Collections.unmodifiableMap(map);
        this.messages = newMessages;
        this.zone = newZone;
        this.catchUpMissed = catchUp;
        // Laufende Events, die aus der Config entfernt wurden, enden still.
        running.keySet().removeIf(id -> {
            if (!map.containsKey(id)) {
                logger.warn("Event " + id + " läuft, ist aber nicht mehr konfiguriert – beendet");
                changed = true;
                return true;
            }
            return false;
        });
    }

    public void setHost(EventHost host) {
        this.host = host;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public ZoneId zone() {
        return zone;
    }

    public Collection<EventDefinition> definitions() {
        return definitions.values();
    }

    public Optional<EventDefinition> definition(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public Optional<RunningEvent> running(String id) {
        return Optional.ofNullable(running.get(id));
    }

    public Collection<RunningEvent> running() {
        return List.copyOf(running.values());
    }

    // ------------------------------------------------------------------ Zeitsteuerung

    public void tick(Instant now) {
        try {
            if (lastProcessed == null || now.isBefore(lastProcessed)
                    || Duration.between(lastProcessed, now).compareTo(MAX_GAP) > 0) {
                recover(lastProcessed != null && now.isAfter(lastProcessed) ? lastProcessed : null, now);
            } else if (now.isAfter(lastProcessed)) {
                process(lastProcessed, now);
            }
        } finally {
            // Auch nach einem Fehler weiterrücken, sonst feuern dieselben Auslöser bei jedem Tick erneut.
            lastProcessed = now;
        }
    }

    private void process(Instant from, Instant to) {
        List<Trigger> triggers = new ArrayList<>();
        for (RunningEvent r : running.values()) {
            if (r.end() != null && inWindow(r.end(), from, to)) {
                triggers.add(new Trigger(r.end(), Kind.END, r.id(), r.start(), 0));
            }
        }
        for (EventDefinition def : definitions.values()) {
            if (!def.enabled()) {
                continue;
            }
            for (Instant start : starts(def, from.minus(def.lead()), to)) {
                for (int minutes : def.announceMinutes()) {
                    Instant at = start.minus(Duration.ofMinutes(minutes));
                    if (inWindow(at, from, to)) {
                        triggers.add(new Trigger(at, Kind.ANNOUNCE, def.id(), start, minutes * 60L));
                    }
                }
                for (int s = def.countdownSeconds(); s >= 1; s--) {
                    Instant at = start.minusSeconds(s);
                    if (inWindow(at, from, to)) {
                        triggers.add(new Trigger(at, Kind.COUNTDOWN, def.id(), start, s));
                    }
                }
                if (inWindow(start, from, to)) {
                    triggers.add(new Trigger(start, Kind.START, def.id(), start, 0));
                }
            }
        }
        triggers.sort(Comparator.comparing(Trigger::at).thenComparing(Trigger::kind));

        for (Trigger t : triggers) {
            try {
                fire(t);
            } catch (RuntimeException e) {
                logger.error(t.kind() + " von Event " + t.eventId() + " fehlgeschlagen", e);
            }
        }
        // Enden, die erst durch Starts in diesem Fenster entstanden sind (sehr kurze Events).
        for (RunningEvent r : List.copyOf(running.values())) {
            if (r.end() != null && !r.end().isAfter(to)) {
                end(r, true);
            }
        }
    }

    private void fire(Trigger t) {
        EventDefinition def = definitions.get(t.eventId());
        switch (t.kind()) {
            case END -> {
                RunningEvent r = running.get(t.eventId());
                if (r != null && r.start().equals(t.occurrenceStart())) {
                    end(r, true);
                }
            }
            case ANNOUNCE -> announce(def, t.secondsLeft() / 60);
            case COUNTDOWN -> countdown(def, t.secondsLeft());
            case START -> start(def, t.at(), t.at().plus(def.duration()), false);
        }
    }

    private void recover(Instant last, Instant now) {
        for (RunningEvent r : List.copyOf(running.values())) {
            if (r.end() != null && !r.end().isAfter(now)) {
                logger.info("Event " + r.id() + " ist während der Pause abgelaufen");
                end(r, false);
            }
        }
        for (EventDefinition def : definitions.values()) {
            if (!def.enabled() || def.duration().isZero() || running.containsKey(def.id())) {
                continue;
            }
            List<Instant> starts = starts(def, now.minus(def.duration()), now);
            for (int i = starts.size() - 1; i >= 0; i--) {
                Instant start = starts.get(i);
                Instant end = start.plus(def.duration());
                boolean missed = !start.isAfter(now) && end.isAfter(now)
                        && (last == null || start.isAfter(last));
                if (!missed) {
                    continue;
                }
                if (catchUpMissed) {
                    logger.info("Event " + def.id() + " wird nachgeholt (Start war " + start + ")");
                    start(def, start, end, false);
                } else {
                    logger.info("Event " + def.id() + " verpasst (Start war " + start + ") – übersprungen");
                }
                break;
            }
        }
    }

    // ------------------------------------------------------------------ Manuelle Steuerung

    /** Startet ein Event sofort, auch wenn es deaktiviert ist. {@code false}, wenn es schon läuft. */
    public boolean startNow(String id, Instant now) {
        EventDefinition def = definitions.get(id);
        if (def == null || running.containsKey(id)) {
            return false;
        }
        return start(def, now, now.plus(def.duration()), true);
    }

    /** Beendet ein laufendes Event mit End-Aktionen. */
    public boolean stopNow(String id) {
        RunningEvent r = running.get(id);
        if (r == null) {
            return false;
        }
        end(r, true);
        return true;
    }

    /** Nächster geplanter Start nach {@code now} innerhalb eines Jahres. */
    public Optional<Instant> nextStart(String id, Instant now) {
        EventDefinition def = definitions.get(id);
        if (def == null || !def.enabled()) {
            return Optional.empty();
        }
        return starts(def, now, now.plus(Duration.ofDays(400))).stream()
                .filter(start -> start.isAfter(now))
                .findFirst();
    }

    /** Produkt eines Modifikators über alle laufenden Events (1.0, wenn keins ihn setzt). */
    public double modifier(String key) {
        double value = 1.0;
        for (RunningEvent r : running.values()) {
            EventDefinition def = definitions.get(r.id());
            if (def != null) {
                value *= def.modifiers().getOrDefault(key, 1.0);
            }
        }
        return value;
    }

    // ------------------------------------------------------------------ Persistenz

    public EventState snapshot() {
        return new EventState(lastProcessed, List.copyOf(running.values()));
    }

    public void restore(EventState state) {
        running.clear();
        state.running().forEach(r -> running.put(r.id(), r));
        lastProcessed = state.lastProcessed();
        changed = false;
    }

    /** {@code true}, wenn sich seit dem letzten Aufruf laufende Events geändert haben. */
    public boolean consumeChanged() {
        boolean was = changed;
        changed = false;
        return was;
    }

    // ------------------------------------------------------------------ intern

    private List<Instant> starts(EventDefinition def, Instant from, Instant to) {
        LocalDate fromDate = from.atZone(zone).toLocalDate().minusDays(1);
        LocalDate toDate = to.atZone(zone).toLocalDate().plusDays(1);
        return def.schedule().startsBetween(fromDate, toDate, zone);
    }

    private static boolean inWindow(Instant at, Instant fromExclusive, Instant toInclusive) {
        return at.isAfter(fromExclusive) && !at.isAfter(toInclusive);
    }

    private boolean start(EventDefinition def, Instant start, Instant end, boolean manual) {
        if (running.containsKey(def.id())) {
            logger.warn("Event " + def.id() + " läuft bereits – Start übersprungen");
            return false;
        }
        RunningEvent r = new RunningEvent(def.id(), start, def.duration().isZero() ? start : end, manual);
        logger.info("Event " + def.id() + " startet" + (manual ? " (manuell)" : ""));
        runActions(def, def.onStart(), Map.of());
        if (!def.duration().isZero()) {
            running.put(def.id(), r);
            changed = true;
        }
        notifyListeners(def, l -> l.started(def, r));
        return true;
    }

    private void end(RunningEvent r, boolean runActions) {
        running.remove(r.id());
        changed = true;
        EventDefinition def = definitions.get(r.id());
        logger.info("Event " + r.id() + " endet");
        if (def != null) {
            if (runActions) {
                runActions(def, def.onEnd(), Map.of());
            }
            notifyListeners(def, l -> l.ended(def, r));
        }
    }

    private void notifyListeners(EventDefinition def, Consumer<Listener> call) {
        for (Listener listener : listeners) {
            try {
                call.accept(listener);
            } catch (RuntimeException e) {
                logger.error("Listener für Event " + def.id() + " fehlgeschlagen", e);
            }
        }
    }

    private void announce(EventDefinition def, long minutes) {
        if (!messages.announce().isEmpty()) {
            host.broadcast(Messages.colorize(fill(messages.announce(), def, Map.of("minutes", Long.toString(minutes)))));
        }
        if (!messages.announceSound().isEmpty()) {
            host.playSound(messages.announceSound(), 1f, 1f);
        }
    }

    private void countdown(EventDefinition def, long seconds) {
        Map<String, String> extra = Map.of("seconds", Long.toString(seconds));
        if (!messages.countdownTitle().isEmpty() || !messages.countdownSubtitle().isEmpty()) {
            host.title(Messages.colorize(fill(messages.countdownTitle(), def, extra)),
                    Messages.colorize(fill(messages.countdownSubtitle(), def, extra)), 0, 25, 5);
        }
        if (!messages.countdownSound().isEmpty()) {
            host.playSound(messages.countdownSound(), 1f, seconds <= 3 ? 2f : 1.5f);
        }
    }

    private String fill(String template, EventDefinition def, Map<String, String> extra) {
        return new ActionContext(host, def.id(), placeholders(def, extra)).fill(template);
    }

    private Map<String, String> placeholders(EventDefinition def, Map<String, String> extra) {
        Map<String, String> values = new HashMap<>();
        values.put("id", def.id());
        values.put("name", def.name());
        values.put("duration", Long.toString(def.duration().toMinutes()));
        values.putAll(extra);
        return values;
    }

    private void runActions(EventDefinition def, List<EventAction> actions, Map<String, String> extra) {
        ActionContext ctx = new ActionContext(host, def.id(), placeholders(def, extra));
        for (EventAction action : actions) {
            try {
                action.run(ctx);
            } catch (RuntimeException e) {
                logger.error("Aktion in Event " + def.id() + " fehlgeschlagen", e);
            }
        }
    }
}
