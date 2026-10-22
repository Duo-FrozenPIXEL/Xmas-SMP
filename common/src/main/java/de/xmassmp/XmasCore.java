package de.xmassmp;

import de.xmassmp.ability.AbilityControls;
import de.xmassmp.ability.AbilityRegistry;
import de.xmassmp.ability.AbilityService;
import de.xmassmp.admin.AdminService;
import de.xmassmp.admin.AuditLog;
import de.xmassmp.config.ConfigManager;
import de.xmassmp.crystal.CrystalRegistry;
import de.xmassmp.crystal.CrystalService;
import de.xmassmp.energy.EnergyService;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.event.EventClock;
import de.xmassmp.event.EventService;
import de.xmassmp.event.action.EventActionRegistry;
import de.xmassmp.hud.HudService;
import de.xmassmp.platform.Platform;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.player.PlayerDataStore;
import de.xmassmp.player.PlayerListener;

import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
    private final CrystalService crystalService;
    private final EnergyService energy;
    private final HudService hud;
    private final List<PlayerListener> playerListeners = new CopyOnWriteArrayList<>();
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
                () -> config.get().effects.maxParticlesPerTick, () -> config.get().effects.maxDisplays,
                new Random(), logger);
        this.events = new EventService(platform.configDirectory(), platform.dataDirectory(),
                platform.eventHost(), eventActions, new EventClock(clock), logger);
        this.energy = new EnergyService(players, config::get, () -> events.modifier(EnergyService.ENERGY_MODIFIER),
                clock::millis, effects, this::syncCrystalItem);
        this.abilityService = new AbilityService(config::get, effects, clock::millis,
                () -> events.modifier(AbilityService.COOLDOWN_MODIFIER), energy::strength);
        this.audit = new AuditLog(platform.dataDirectory(), () -> config.get().audit.enabled, clock, logger);
        this.crystalService = new CrystalService(platform, crystals, players, config::get,
                energy::stage, effects, new Random());
        this.admin = new AdminService(crystals, players, config, events, audit, this::reload, crystalService::sync);
        this.hud = new HudService(platform, crystals, abilities, players, config::get, energy::stage, effects,
                clock::millis);
        addPlayerListener(crystalService);
        addPlayerListener(energy);
        addPlayerListener(hud);
        addPlayerListener(new AbilityControls(crystals, abilities, abilityService, players, config::get, hud,
                clock::millis));
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
        effects.tick();
        crystalService.tick(ticks);
        hud.tick(ticks);
        if (ticks % TICKS_PER_SECOND == 0) {
            events.tick();
        }
        if (ticks % AUTOSAVE_TICKS == 0) {
            players.saveDirty();
        }
    }

    /** Eigene Reaktionen auf Spieler-Ereignisse; Reihenfolge = Registrierungsreihenfolge. */
    public void addPlayerListener(PlayerListener listener) {
        playerListeners.add(listener);
    }

    // ------------------------------------------------------------------ Spieler-Ereignisse (vom Adapter)

    public void onPlayerJoin(PlayerHandle player) {
        players.get(player.uuid()).pruneCooldowns(clock.millis());
        dispatch("Join", l -> l.onJoin(player));
    }

    public void onPlayerQuit(UUID uuid) {
        // Auch bei abgeschaltetem Plugin, damit keine Daten verloren gehen.
        forEachListener("Quit", l -> l.onQuit(uuid));
        players.unload(uuid);
    }

    /** @param killer verursachender Spieler oder {@code null} */
    public void onPlayerDeath(PlayerHandle victim, PlayerHandle killer) {
        PlayerHandle realKiller = killer != null && killer.uuid().equals(victim.uuid()) ? null : killer;
        dispatch("Tod", l -> l.onDeath(victim, realKiller));
    }

    public void onPlayerRespawn(PlayerHandle player) {
        dispatch("Respawn", l -> l.onRespawn(player));
    }

    /** @return {@code true}, wenn der Adapter den Zweithand-Tausch abbrechen soll */
    public boolean onSwapHands(PlayerHandle player) {
        return firstHandled("Zweithand-Tausch", l -> l.onSwapHands(player));
    }

    /** @return {@code true}, wenn der Adapter das Vanilla-Verhalten unterdrücken soll */
    public boolean onUseItem(PlayerHandle player, Map<String, String> itemData) {
        return firstHandled("Item-Nutzung", l -> l.onUseItem(player, itemData));
    }

    /** Für Dienste, die vor dem {@link CrystalService} entstehen. */
    private void syncCrystalItem(UUID uuid) {
        crystalService.sync(uuid);
    }

    private void dispatch(String event, Consumer<PlayerListener> call) {
        if (config.get().general.enabled) {
            forEachListener(event, call);
        }
    }

    private void forEachListener(String event, Consumer<PlayerListener> call) {
        for (PlayerListener listener : playerListeners) {
            try {
                call.accept(listener);
            } catch (RuntimeException e) {
                logger.error("Fehler beim Verarbeiten von " + event + " in " + listener.getClass().getName(), e);
            }
        }
    }

    private boolean firstHandled(String event, Predicate<PlayerListener> call) {
        if (!config.get().general.enabled) {
            return false;
        }
        for (PlayerListener listener : playerListeners) {
            try {
                if (call.test(listener)) {
                    return true;
                }
            } catch (RuntimeException e) {
                logger.error("Fehler beim Verarbeiten von " + event + " in " + listener.getClass().getName(), e);
            }
        }
        return false;
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

    public CrystalService crystalService() {
        return crystalService;
    }

    public EnergyService energy() {
        return energy;
    }

    public HudService hud() {
        return hud;
    }
}
