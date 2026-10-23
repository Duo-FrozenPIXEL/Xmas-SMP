package de.xmassmp.ability;

import de.xmassmp.config.XmasConfig;
import de.xmassmp.crystal.Crystal;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.player.PlayerData;

import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/** Prüft Freischaltung, Schalter und Cooldown und löst aktive Fähigkeiten aus. */
public final class AbilityService {

    /** Schlüssel des Event-Modifikators, der alle Cooldowns skaliert. */
    public static final String COOLDOWN_MODIFIER = "cooldown_multiplier";

    /** {@code BROKEN}: Kristall ohne Energie, Fähigkeiten ruhen. */
    public enum Result { ACTIVATED, DISABLED, LOCKED, BROKEN, ON_COOLDOWN }

    private final Supplier<XmasConfig> config;
    private final EffectEngine effects;
    private final LongSupplier clockMillis;
    private final DoubleSupplier eventCooldownMultiplier;
    private final ToDoubleFunction<PlayerData> strength;

    /**
     * @param strength Energie-Stärke eines Spielers; 0 schaltet seine Fähigkeiten ab
     */
    public AbilityService(Supplier<XmasConfig> config, EffectEngine effects, LongSupplier clockMillis,
                          DoubleSupplier eventCooldownMultiplier, ToDoubleFunction<PlayerData> strength) {
        this.config = config;
        this.effects = effects;
        this.clockMillis = clockMillis;
        this.eventCooldownMultiplier = eventCooldownMultiplier;
        this.strength = strength;
    }

    public Result tryActivate(PlayerHandle player, PlayerData data, Crystal crystal, ActiveAbility ability) {
        XmasConfig cfg = config.get();
        XmasConfig.CrystalSettings settings = cfg.crystal(crystal.id());
        if (!cfg.general.enabled || !cfg.general.abilitiesEnabled || !settings.enabled
                || !crystal.id().equals(data.crystalId())) {
            return Result.DISABLED;
        }
        if (!ability.isUnlocked(data)) {
            return Result.LOCKED;
        }
        double power = strength.applyAsDouble(data);
        if (power <= 0) {
            return Result.BROKEN;
        }
        long now = clockMillis.getAsLong();
        if (data.isOnCooldown(ability.id(), now)) {
            return Result.ON_COOLDOWN;
        }
        data.setCooldownUntil(ability.id(), now + effectiveCooldownMillis(ability, settings));
        ability.activate(new AbilityContext(player, data, crystal, effects, settings.damageMultiplier, power));
        return Result.ACTIVATED;
    }

    /** Cooldown nach Config- und Event-Multiplikatoren, z. B. für die HUD-Anzeige. */
    public long effectiveCooldownMillis(ActiveAbility ability, String crystalId) {
        return effectiveCooldownMillis(ability, config.get().crystal(crystalId));
    }

    long effectiveCooldownMillis(ActiveAbility ability, XmasConfig.CrystalSettings settings) {
        double factor = settings.cooldownMultiplier * eventCooldownMultiplier.getAsDouble();
        return Math.max(0, Math.round(ability.cooldown().toMillis() * factor));
    }
}
