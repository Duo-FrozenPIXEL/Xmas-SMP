package de.xmassmp;

import de.xmassmp.ability.AbilityRegistry;
import de.xmassmp.ability.AbilityService;
import de.xmassmp.admin.AdminService;
import de.xmassmp.admin.AuditLog;
import de.xmassmp.config.ConfigManager;
import de.xmassmp.crystal.CrystalRegistry;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.event.EventClock;
import de.xmassmp.event.EventService;
import de.xmassmp.event.action.EventActionRegistry;
import de.xmassmp.platform.Platform;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.player.PlayerDataStore;

import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Wurzel des plattformneutralen Kerns. Der Adapter erzeugt ihn beim Serverstart, ruft
 * {@link #tick()} jeden Server-Tick und {@link #shutdown()} beim Stoppen auf.
 */
public final class XmasCore {

    public static final String MOD_ID = "xmassmp";

    private static final int TICKS_PER_SECOND = 20;
    private static final int AUTOSAVE_TICKS = 5 * 60 * TICKS_PER_SECOND;

    private final Platform platform;
    private final XmasLogger logger;
    private final Clock clock;
    private final ConfigManager config;
    private final CrystalRegistry crystals;
    private final AbilityRegistry abilities;
    private final EventActionRegistry eventActions;
    private final PlayerDataStore players;
    private final EffectEngine effects;
    private final EventService events;
    private final AbilityService abilityService;
    private final AuditLog audit;
    private final AdminService admin;
    private final Set<String> failing = new HashSet<>();
    private long ticks;

    public XmasCore(Platform platform) {
        this(platform, Clock.systemUTC());
    }

    public XmasCore(Platform platform, Clock clock) {
        this.platform = platform;
        this.logger = platform.logger();
        this.clock = clock;
        this.config = new ConfigManager(platform.configDirectory());
        this.crystals = CrystalRegistry.withDefaults();
        this.abilities = new AbilityRegistry();
        this.eventActions = EventActionRegistry.withDefaults();
        this.players = new PlayerDataStore(platform.dataDirectory(), () -> config.get().energy.start, logger);
        this.effects = new EffectEngine(platform.effectSink(), () -> config.get().effects.quality,
                () -> config.get().effects.maxParticlesPerTick, new Random(), logger);
        this.events = new EventService(platform.configDirectory(), platform.dataDirectory(),
                platform.eventHost(), eventActions, new EventClock(clock), logger);
        this.abilityService = new AbilityService(config::get, effects, clock::millis,
                () -> events.modifier(AbilityService.COOLDOWN_MODIFIER));
        this.audit = new AuditLog(platform.dataDirectory(), () -> config.get().audit.enabled, clock, logger);
        this.admin = new AdminService(crystals, players, config, events, audit, this::reload);
    }

    /** Lädt Konfiguration und Events. Erst nach der Registrierung eigener Erweiterungen aufrufen. */
    public void start() {
        reload().forEach(logger::info);
        logger.info("Xmas-SMP gestartet (" + crystals.ids().size() + " Kristalle)");
    }

    /** Lädt {@code config.jsonc} und {@code events.jsonc} neu und liefert die Meldungen. */
    public List<String> reload() {
        List<String> messages = new ArrayList<>(config.load().messages());
        var cfg = config.get();
        messages.addAll(events.reload(ZoneId.of(cfg.events.timezone), cfg.events.catchUpMissed));
        return messages;
    }

    /** Einmal pro Server-Tick aufrufen. */
    public void tick() {
        ticks++;
        if (!config.get().general.enabled) {
            return;
        }
        guarded("Effekte", effects::tick);
        if (ticks % TICKS_PER_SECOND == 0) {
            guarded("Events", events::tick);
        }
        if (ticks % AUTOSAVE_TICKS == 0) {
            guarded("Autosave", players::saveDirty);
        }
    }

    /**
     * Fängt Fehler ab, damit ein einzelner Teil nicht den Server-Tick und damit den Server
     * abbrechen lässt. Geloggt wird nur der erste Fehler, bis der Teil wieder durchläuft.
     */
    private void guarded(String part, Runnable task) {
        try {
            task.run();
            failing.remove(part);
        } catch (RuntimeException e) {
            if (failing.add(part)) {
                logger.error(part + " im Server-Tick fehlgeschlagen (weitere Fehler bis zur Erholung "
                        + "werden nicht geloggt)", e);
            }
        }
    }

    public void onPlayerJoin(UUID uuid) {
        players.get(uuid).pruneCooldowns(clock.millis());
    }

    public void onPlayerQuit(UUID uuid) {
        players.unload(uuid);
    }

    public void shutdown() {
        effects.cancelAll();
        events.shutdown();
        players.saveAndUnloadAll();
        logger.info("Xmas-SMP gestoppt");
    }

    public Platform platform() {
        return platform;
    }

    public ConfigManager config() {
        return config;
    }

    public CrystalRegistry crystals() {
        return crystals;
    }

    public AbilityRegistry abilities() {
        return abilities;
    }

    public EventActionRegistry eventActions() {
        return eventActions;
    }

    public PlayerDataStore players() {
        return players;
    }

    public EffectEngine effects() {
        return effects;
    }

    public EventService events() {
        return events;
    }

    public AbilityService abilityService() {
        return abilityService;
    }

    public AuditLog audit() {
        return audit;
    }

    public AdminService admin() {
        return admin;
    }
}
