package de.xmassmp.ability;

import de.xmassmp.config.XmasConfig;
import de.xmassmp.crystal.Crystal;
import de.xmassmp.crystal.CrystalRegistry;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.platform.Platform;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.player.PlayerData;
import de.xmassmp.player.PlayerDataStore;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * Wendet die Passiven an. Laut Owner wirken sie <b>immer</b>: auch bei 0 Energie und egal, wo der
 * Kristall im Inventar liegt – es zählen nur die Spielerdaten (Kristall und Tier).
 */
public final class PassiveService {

    private final Platform platform;
    private final CrystalRegistry crystals;
    private final AbilityRegistry abilities;
    private final PlayerDataStore players;
    private final Supplier<XmasConfig> config;
    private final EffectEngine effects;
    private final ToDoubleFunction<PlayerData> strength;

    /** @param strength Energie-Stärke; bei 0 Energie wirken Passive mit {@code energy.minStrength} */
    public PassiveService(Platform platform, CrystalRegistry crystals, AbilityRegistry abilities,
                          PlayerDataStore players, Supplier<XmasConfig> config, EffectEngine effects,
                          ToDoubleFunction<PlayerData> strength) {
        this.platform = platform;
        this.crystals = crystals;
        this.abilities = abilities;
        this.players = players;
        this.config = config;
        this.effects = effects;
        this.strength = strength;
    }

    /** Einmal pro Server-Tick. */
    public void tick(long ticks) {
        XmasConfig cfg = config.get();
        if (!cfg.general.abilitiesEnabled) {
            return;
        }
        for (PlayerHandle player : platform.onlinePlayers()) {
            PlayerData data = players.get(player.uuid());
            Optional<Crystal> crystal = crystals.get(data.crystalId());
            if (crystal.isEmpty() || !cfg.crystal(crystal.get().id()).enabled || !player.isAlive()) {
                continue;
            }
            for (Ability ability : abilities.forCrystal(crystal.get().id())) {
                if (ability instanceof PassiveAbility passive && passive.isUnlocked(data)
                        && ticks % Math.max(1, passive.tickInterval()) == 0) {
                    double power = strength.applyAsDouble(data);
                    passive.tick(new AbilityContext(player, data, crystal.get(), effects,
                            cfg.crystal(crystal.get().id()).damageMultiplier,
                            power > 0 ? power : cfg.energy.minStrength));
                }
            }
        }
    }
}
