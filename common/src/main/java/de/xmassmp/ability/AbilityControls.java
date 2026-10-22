package de.xmassmp.ability;

import de.xmassmp.config.XmasConfig;
import de.xmassmp.crystal.Crystal;
import de.xmassmp.crystal.CrystalRegistry;
import de.xmassmp.hud.HudService;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.player.PlayerData;
import de.xmassmp.player.PlayerDataStore;
import de.xmassmp.player.PlayerListener;

import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Löst Fähigkeiten über die Zweithand-Taste aus, ganz ohne Client-Mod: {@code F} = Fähigkeit 1,
 * Schleichen + {@code F} = Fähigkeit 2. Für Spieler mit Kristall ersetzt das den normalen
 * Zweithand-Tausch ({@code controls.swapHandsActivates}), sofern der Kristall aktive Kräfte hat.
 */
public final class AbilityControls implements PlayerListener {

    private final CrystalRegistry crystals;
    private final AbilityRegistry abilities;
    private final AbilityService service;
    private final PlayerDataStore players;
    private final Supplier<XmasConfig> config;
    private final HudService hud;
    private final LongSupplier clockMillis;

    public AbilityControls(CrystalRegistry crystals, AbilityRegistry abilities, AbilityService service,
                           PlayerDataStore players, Supplier<XmasConfig> config, HudService hud,
                           LongSupplier clockMillis) {
        this.crystals = crystals;
        this.abilities = abilities;
        this.service = service;
        this.players = players;
        this.config = config;
        this.hud = hud;
        this.clockMillis = clockMillis;
    }

    @Override
    public boolean onSwapHands(PlayerHandle player) {
        if (!config.get().controls.swapHandsActivates) {
            return false;
        }
        PlayerData data = players.get(player.uuid());
        Optional<Crystal> crystal = crystals.get(data.crystalId());
        if (crystal.isEmpty()) {
            return false;
        }
        String crystalId = crystal.get().id();
        if (abilities.active(crystalId, 1).isEmpty() && abilities.active(crystalId, 2).isEmpty()) {
            // Kristall ohne aktive Kräfte (noch nicht umgesetzt): F tauscht wie gewohnt.
            return false;
        }
        int slot = player.isSneaking() ? 2 : 1;
        Optional<ActiveAbility> ability = abilities.active(crystalId, slot);
        if (ability.isEmpty()) {
            hud.flash(player, "§7Fähigkeit " + slot + " gibt es noch nicht.");
            return true;
        }
        AbilityService.Result result = service.tryActivate(player, data, crystal.get(), ability.get());
        String feedback = switch (result) {
            case ACTIVATED -> null;
            case DISABLED -> "§7Fähigkeiten sind gerade abgeschaltet.";
            case LOCKED -> "§7Fähigkeit " + slot + " schaltest du mit Tier " + ability.get().requiredTier() + " frei.";
            case BROKEN -> "§cDein Kristall ist zerbrochen – sammle Energie durch PvP-Siege.";
            case ON_COOLDOWN -> "§7Noch §f" + HudService.formatSeconds(
                    data.remainingCooldownMillis(ability.get().id(), clockMillis.getAsLong())) + " §7Abklingzeit.";
        };
        if (feedback != null) {
            hud.flash(player, feedback);
        }
        return true;
    }
}
