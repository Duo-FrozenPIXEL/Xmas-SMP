package de.xmassmp.energy;

import de.xmassmp.config.XmasConfig;
import de.xmassmp.crystal.PristineStage;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.effect.EffectTimeline;
import de.xmassmp.effect.SoundStack;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.player.PlayerData;
import de.xmassmp.player.PlayerDataStore;
import de.xmassmp.player.PlayerListener;
import de.xmassmp.text.Messages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Breaking-Pristine-System: Energie steigt bei PvP-Kills, sinkt bei jedem Tod und bestimmt
 * Bruchstufe (Aussehen des Kristalls), Zugriff auf die aktiven Fähigkeiten und deren Stärke.
 *
 * <p>Regeln laut Owner (docs/implementierungsplan.md, 1.1): Wer ein Opfer ohne Energie besiegt,
 * bekommt nichts. Hat der Sieger schon das Maximum, fällt die Energie als Energy Bottle zu Boden.
 * Jede Änderung landet im {@link EnergyLedger}.
 */
public final class EnergyService implements PlayerListener {

    /** Event-Modifikator, der den Energiegewinn bei Kills multipliziert. */
    public static final String ENERGY_MODIFIER = "energy_multiplier";

    /** Reaktion auf einen Wechsel der Bruchstufe, z. B. für eine Bruch-Animation. */
    @FunctionalInterface
    public interface StageListener {
        void onStageChanged(PlayerHandle player, PristineStage from, PristineStage to);
    }

    /** Energie, die der Sieger nicht mehr aufnehmen kann (er hat schon das Maximum). */
    @FunctionalInterface
    public interface OverflowHandler {
        void onOverflow(PlayerHandle killer, PlayerHandle victim, int amount);
    }

    private record KillPair(UUID killer, UUID victim) {
    }

    private final PlayerDataStore players;
    private final Supplier<XmasConfig> config;
    private final DoubleSupplier eventMultiplier;
    private final LongSupplier clockMillis;
    private final EffectEngine effects;
    private final Consumer<UUID> changed;
    private final EnergyLedger ledger;
    private OverflowHandler overflow = (killer, victim, amount) -> {
    };
    private final List<StageListener> stageListeners = new CopyOnWriteArrayList<>();
    /** Letzter Energiegewinn je Sieger/Besiegtem; nur im Speicher, ein Neustart setzt ihn zurück. */
    private final Map<KillPair, Long> lastRewardedKill = new HashMap<>();

    /**
     * @param changed wird nach jeder Energieänderung aufgerufen (z. B. Item-Abgleich)
     */
    public EnergyService(PlayerDataStore players, Supplier<XmasConfig> config, DoubleSupplier eventMultiplier,
                         LongSupplier clockMillis, EffectEngine effects, Consumer<UUID> changed) {
        this(players, config, eventMultiplier, clockMillis, effects, changed, EnergyLedger.NONE);
    }

    public EnergyService(PlayerDataStore players, Supplier<XmasConfig> config, DoubleSupplier eventMultiplier,
                         LongSupplier clockMillis, EffectEngine effects, Consumer<UUID> changed,
                         EnergyLedger ledger) {
        this.ledger = ledger;
        this.players = players;
        this.config = config;
        this.eventMultiplier = eventMultiplier;
        this.clockMillis = clockMillis;
        this.effects = effects;
        this.changed = changed;
    }

    public void addStageListener(StageListener listener) {
        stageListeners.add(listener);
    }

    public void setOverflowHandler(OverflowHandler handler) {
        this.overflow = handler;
    }

    public EnergyLedger ledger() {
        return ledger;
    }

    // ------------------------------------------------------------------ Abfragen

    public PristineStage stage(PlayerData data) {
        return stageFor(data.energy(), config.get().energy.stages);
    }

    static PristineStage stageFor(int energy, XmasConfig.Stages stages) {
        if (energy >= stages.pristine) {
            return PristineStage.PRISTINE;
        }
        if (energy >= stages.scratched) {
            return PristineStage.SCRATCHED;
        }
        if (energy >= stages.damaged) {
            return PristineStage.DAMAGED;
        }
        return PristineStage.BROKEN;
    }

    /**
     * Faktor für Schaden, Dauer und Radius der Fähigkeiten: 0 bei zerbrochenem Kristall (Fähigkeiten
     * aus), sonst linear von {@code energy.minStrength} bis 1.0 bei maximaler Energie.
     */
    public double strength(PlayerData data) {
        XmasConfig.Energy cfg = config.get().energy;
        if (stage(data) == PristineStage.BROKEN) {
            return 0;
        }
        int from = cfg.stages.damaged;
        if (cfg.max <= from) {
            return 1;
        }
        double t = (double) (Math.min(data.energy(), cfg.max) - from) / (cfg.max - from);
        return cfg.minStrength + (1 - cfg.minStrength) * Math.max(0, t);
    }

    /**
     * Mindest-Energie für eine aktive Fähigkeit: Slot 1 = Primär, 2 = Sekundär, 3 = Ultimate
     * ({@code energy.access}). Passive brauchen keine Energie.
     */
    public int requiredEnergy(int slot) {
        XmasConfig.Access access = config.get().energy.access;
        return switch (slot) {
            case 1 -> access.primary;
            case 2 -> access.secondary;
            default -> access.ultimate;
        };
    }

    // ------------------------------------------------------------------ Kills und Tode

    @Override
    public void onDeath(PlayerHandle victim, PlayerHandle killer) {
        XmasConfig.Energy cfg = config.get().energy;
        if (!cfg.pvpEnabled) {
            return;
        }
        int before = players.get(victim.uuid()).energy();
        int lost = change(victim, -cfg.deathLoss, EnergyLedger.DEATH, killer == null ? null : killer.name());
        if (lost != 0) {
            victim.sendMessage(Messages.prefixed("§c" + lost + " Energie " + summary(victim.uuid())));
        }
        if (killer == null) {
            return;
        }
        if (before <= cfg.min) {
            // Kein Farmen mit Zweitaccounts: Wer nichts zu verlieren hat, bringt auch nichts.
            killer.sendMessage(Messages.prefixed("Keine Energie: " + victim.name() + " hatte keine Energie mehr."));
            return;
        }
        reward(killer, victim, cfg);
    }

    private void reward(PlayerHandle killer, PlayerHandle victim, XmasConfig.Energy cfg) {
        long now = clockMillis.getAsLong();
        long cooldown = cfg.sameVictimCooldownMinutes * 60_000L;
        KillPair pair = new KillPair(killer.uuid(), victim.uuid());
        lastRewardedKill.values().removeIf(at -> now - at >= cooldown);
        if (cooldown > 0 && lastRewardedKill.containsKey(pair)) {
            killer.sendMessage(Messages.prefixed("Keine Energie: Du hast " + victim.name()
                    + " erst vor kurzem besiegt."));
            return;
        }
        int gain = (int) Math.round(cfg.killGain * Math.max(0, eventMultiplier.getAsDouble()));
        if (gain <= 0) {
            return;
        }
        lastRewardedKill.put(pair, now);
        int gained = change(killer, gain, EnergyLedger.KILL, victim.name());
        if (gained > 0) {
            killer.sendMessage(Messages.prefixed("§a+" + gained + " Energie " + summary(killer.uuid())));
        }
        int rest = gain - gained;
        if (rest > 0 && cfg.dropBottleWhenKillerAtMax) {
            overflow.onOverflow(killer, victim, rest);
        } else if (gained == 0) {
            killer.sendMessage(Messages.prefixed("Deine Energie ist schon voll " + summary(killer.uuid())));
        }
    }

    /** Wie {@link #change(PlayerHandle, int, String, String)} mit Grund {@link EnergyLedger#ADMIN}. */
    public int change(PlayerHandle player, int delta) {
        return change(player, delta, EnergyLedger.ADMIN, null);
    }

    /**
     * Ändert die Energie innerhalb von {@code energy.min}/{@code max}, speichert sofort, protokolliert
     * im {@link EnergyLedger} und meldet einen Stufenwechsel.
     *
     * @param reason Grund fürs Protokoll, siehe Konstanten in {@link EnergyLedger}
     * @param ref    Bezug fürs Protokoll (Flaschen-ID, Gegner …) oder {@code null}
     * @return tatsächliche Änderung (0, wenn die Grenze schon erreicht war)
     */
    public int change(PlayerHandle player, int delta, String reason, String ref) {
        XmasConfig.Energy cfg = config.get().energy;
        PlayerData data = players.get(player.uuid());
        int before = data.energy();
        int after = Math.max(cfg.min, Math.min(cfg.max, before + delta));
        if (after == before) {
            return 0;
        }
        PristineStage oldStage = stage(data);
        data.setEnergy(after);
        players.save(player.uuid());
        ledger.record(player.uuid(), player.name(), before, after, reason, ref);
        PristineStage newStage = stage(data);
        if (newStage != oldStage) {
            announceStage(player, oldStage, newStage);
        }
        changed.accept(player.uuid());
        return after - before;
    }

    private void announceStage(PlayerHandle player, PristineStage from, PristineStage to) {
        boolean worse = to.ordinal() < from.ordinal();
        String name = to.displayName().fallback();
        player.sendMessage(Messages.prefixed(worse
                ? (to == PristineStage.BROKEN
                        ? "§cDein Kristall ist zerbrochen – deine Fähigkeiten ruhen, bis du wieder Energie hast."
                        : "§cDein Kristall ist jetzt " + name + ".")
                : "§aDein Kristall ist jetzt " + name + "."));
        SoundStack sound = worse
                ? SoundStack.builder()
                        .layer("minecraft:block.glass.break", 0.9f, 1.3f, 0.1f, 0)
                        .layer("minecraft:block.amethyst_cluster.break", 0.8f, 0.7f, 0.1f, 2)
                        .build()
                : SoundStack.builder()
                        .layer("minecraft:block.amethyst_block.chime", 1f, 1.4f, 0.15f, 0)
                        .layer("minecraft:block.beacon.power_select", 0.5f, 1.8f)
                        .build();
        effects.play(EffectTimeline.builder().sound(0, sound).build(), player.world(), player.position());
        for (StageListener listener : stageListeners) {
            listener.onStageChanged(player, from, to);
        }
    }

    private String summary(UUID uuid) {
        return "§7(" + players.get(uuid).energy() + "/" + config.get().energy.max + ")";
    }
}
