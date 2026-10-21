package de.xmassmp.effect;

import java.util.ArrayList;
import java.util.List;

/**
 * Unveränderliche Animation über mehrere Ticks. Typischer Aufbau einer Kraft:
 *
 * <pre>{@code
 * EffectTimeline.builder()
 *     .phase(20, Easing.EASE_IN, (c, p, f) -> ...)   // Aufladen
 *     .phase(1, (c, p, f) -> ...)                    // Auslösen
 *     .phase(30, Easing.EASE_OUT, (c, p, f) -> ...)  // Nachklingen
 *     .build();
 * }</pre>
 */
public final class EffectTimeline {

    /** Einmalige Aktion. */
    @FunctionalInterface
    public interface Step {
        void run(EffectCanvas canvas);
    }

    /**
     * Wird in jedem Tick eines Abschnitts aufgerufen.
     *
     * @param progress Fortschritt 0–1 nach Easing (erster Tick 0, letzter 1)
     * @param frame    Tick innerhalb des Abschnitts, ab 0
     */
    @FunctionalInterface
    public interface Frame {
        void run(EffectCanvas canvas, double progress, int frame);
    }

    record Entry(int start, int duration, Easing easing, Frame frame) {
        boolean activeAt(int age) {
            return age >= start && age < start + duration;
        }
    }

    private final List<Entry> entries;
    private final int length;

    private EffectTimeline(List<Entry> entries) {
        this.entries = List.copyOf(entries);
        this.length = entries.stream().mapToInt(e -> e.start() + e.duration()).max().orElse(0);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Gesamtlänge in Ticks. */
    public int length() {
        return length;
    }

    void runTick(int age, EffectCanvas canvas) {
        for (Entry entry : entries) {
            if (entry.activeAt(age)) {
                int frame = age - entry.start();
                double linear = entry.duration() <= 1 ? 1 : (double) frame / (entry.duration() - 1);
                entry.frame().run(canvas, entry.easing().apply(linear), frame);
            }
        }
    }

    public static final class Builder {
        private final List<Entry> entries = new ArrayList<>();
        private int cursor;

        /** Einmalige Aktion bei {@code tick}. */
        public Builder at(int tick, Step step) {
            return during(tick, 1, Easing.LINEAR, (canvas, progress, frame) -> step.run(canvas));
        }

        /** Sound-Stack bei {@code tick}. */
        public Builder sound(int tick, SoundStack sounds) {
            return at(tick, canvas -> canvas.sound(sounds));
        }

        /** Abschnitt an fester Position, darf sich mit anderen überlappen. */
        public Builder during(int start, int duration, Easing easing, Frame frame) {
            if (start < 0 || duration < 1) {
                throw new IllegalArgumentException("start ≥ 0 und duration ≥ 1 erwartet");
            }
            entries.add(new Entry(start, duration, easing, frame));
            return this;
        }

        public Builder during(int start, int duration, Frame frame) {
            return during(start, duration, Easing.LINEAR, frame);
        }

        /** Hängt einen Abschnitt hinter den vorherigen {@code phase}-Aufruf an. */
        public Builder phase(int duration, Easing easing, Frame frame) {
            during(cursor, duration, easing, frame);
            cursor += duration;
            return this;
        }

        public Builder phase(int duration, Frame frame) {
            return phase(duration, Easing.LINEAR, frame);
        }

        /** Startpunkt des nächsten {@code phase}-Aufrufs (für Sounds/Overlays an Phasengrenzen). */
        public int cursor() {
            return cursor;
        }

        public EffectTimeline build() {
            return new EffectTimeline(entries);
        }
    }
}
