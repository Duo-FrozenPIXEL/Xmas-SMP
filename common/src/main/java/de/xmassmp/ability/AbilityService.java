package de.xmassmp.ability;

import de.xmassmp.config.XmasConfig;
import de.xmassmp.crystal.Crystal;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.player.PlayerData;

import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Prüft Freischaltung, Schalter und Cooldown und löst aktive Fähigkeiten aus. */
public final class AbilityService {

    /** Schlüssel des Event-Modifikators, der alle Cooldowns skaliert. */
    public static final String COOLDOWN_MODIFIER = "cooldown_multiplier";

    public enum Result { ACTIVATED, DISABLED, LOCKED, ON_COOLDOWN }

    private final Supplier<XmasConfig> config;
    private final EffectEngine effects;
    private final LongSupplier clockMillis;
    private final DoubleSupplier eventCooldownMultiplier;

    public AbilityService(Supplier<XmasConfig> config, EffectEngine effects, LongSupplier clockMillis,
                          DoubleSupplier eventCooldownMultiplier) {
        this.config = config;
        this.effects = effects;
        this.clockMillis = clockMillis;
        this.eventCooldownMultiplier = eventCooldownMultiplier;
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
        long now = clockMillis.getAsLong();
        if (data.isOnCooldown(ability.id(), now)) {
            return Result.ON_COOLDOWN;
        }
        data.setCooldownUntil(ability.id(), now + effectiveCooldownMillis(ability, settings));
        ability.activate(new AbilityContext(player, data, crystal, effects, settings.damageMultiplier));
        return Result.ACTIVATED;
    }

    long effectiveCooldownMillis(ActiveAbility ability, XmasConfig.CrystalSettings settings) {
        double factor = settings.cooldownMultiplier * eventCooldownMultiplier.getAsDouble();
        return Math.max(0, Math.round(ability.cooldown().toMillis() * factor));
    }
}
