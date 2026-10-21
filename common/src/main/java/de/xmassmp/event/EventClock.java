package de.xmassmp.event;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Uhr des Event-Systems. Im Testmodus läuft sie mit einem Versatz zur echten Zeit weiter. */
public final class EventClock {

    private final Clock base;
    private Duration offset;

    public EventClock(Clock base) {
        this.base = base;
    }

    public Instant now() {
        Instant real = base.instant();
        return offset == null ? real : real.plus(offset);
    }

    public Instant realNow() {
        return base.instant();
    }

    /** Ab jetzt so tun, als wäre es {@code target}; die Zeit läuft normal weiter. */
    public void simulate(Instant target) {
        offset = Duration.between(base.instant(), target);
    }

    public void reset() {
        offset = null;
    }

    public boolean isSimulated() {
        return offset != null;
    }
}
