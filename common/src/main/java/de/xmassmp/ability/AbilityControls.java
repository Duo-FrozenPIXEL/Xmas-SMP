package de.xmassmp.ability;

import de.xmassmp.config.XmasConfig;
import de.xmassmp.crystal.Crystal;
import de.xmassmp.crystal.CrystalRegistry;
import de.xmassmp.hud.HudService;
import de.xmassmp.item.ItemCatalog;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.player.PlayerData;
import de.xmassmp.player.PlayerDataStore;
import de.xmassmp.player.PlayerListener;

import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Löst Fähigkeiten aus, ganz ohne Client-Mod:
 * <ul>
 *   <li>{@code F} (Zweithand-Taste) = Primär, Schleichen + {@code F} = Sekundär
 *       ({@code controls.swapHandsActivates}). Der normale Zweithand-Tausch ist dann für Spieler mit
 *       Kristall abgeschaltet, sofern der Kristall aktive Kräfte hat.</li>
 *   <li>Schleichen + Rechtsklick mit dem eigenen Kristall in der Haupthand = Ultimate
 *       ({@code controls.sneakUseUltimate}).</li>
 *   <li>Mit dem optionalen Client-Mod frei belegbare Tasten ({@link #onKey}). Der Server prüft dabei
 *       alles selbst; der Mod meldet nur die Slot-Nummer.</li>
 * </ul>
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
        Optional<Crystal> crystal = crystals.get(players.get(player.uuid()).crystalId());
        if (crystal.isEmpty() || !hasActiveAbilities(crystal.get())) {
            // Kristall ohne aktive Kräfte (noch nicht umgesetzt): F tauscht wie gewohnt.
            return false;
        }
        activate(player, player.isSneaking() ? 2 : 1);
        return true;
    }

    @Override
    public boolean onUseItem(PlayerHandle player, Map<String, String> itemData) {
        if (!config.get().controls.sneakUseUltimate || !player.isSneaking()
                || !ItemCatalog.isOwnedCrystal(itemData)
                || !player.uuid().toString().equals(itemData.get(ItemCatalog.DATA_OWNER))) {
            return false;
        }
        activate(player, 3);
        return true;
    }

    /**
     * Taste aus dem Client-Mod.
     *
     * @return {@code false}, wenn der Slot ungültig ist oder Client-Tasten abgeschaltet sind
     */
    public boolean onKey(PlayerHandle player, int slot) {
        if (!config.get().controls.clientKeys || slot < 1 || slot > Crystal.SLOTS) {
            return false;
        }
        if (crystals.get(players.get(player.uuid()).crystalId()).isEmpty()) {
            return false;
        }
        activate(player, slot);
        return true;
    }

    private boolean hasActiveAbilities(Crystal crystal) {
        for (int slot = 1; slot <= Crystal.SLOTS; slot++) {
            if (abilities.active(crystal.id(), slot).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private void activate(PlayerHandle player, int slot) {
        PlayerData data = players.get(player.uuid());
        Optional<Crystal> crystal = crystals.get(data.crystalId());
        if (crystal.isEmpty()) {
            return;
        }
        Optional<ActiveAbility> ability = abilities.active(crystal.get().id(), slot);
        if (ability.isEmpty()) {
            hud.flash(player, "§7" + slotName(slot) + " gibt es noch nicht.");
            return;
        }
        AbilityService.Result result = service.tryActivate(player, data, crystal.get(), ability.get());
        String feedback = switch (result) {
            case ACTIVATED -> null;
            case DISABLED -> "§7Fähigkeiten sind gerade abgeschaltet.";
            case LOCKED -> "§7" + slotName(slot) + " schaltest du mit Tier " + AbilityService.requiredTier(ability.get())
                    + " frei.";
            case BROKEN -> "§cDein Kristall ist zerbrochen – sammle Energie durch PvP-Siege.";
            case LOW_ENERGY -> "§c" + slotName(slot) + " braucht mindestens " + service.requiredEnergy(slot)
                    + " Energie.";
            case ON_COOLDOWN -> "§7Noch §f" + HudService.formatSeconds(
                    data.remainingCooldownMillis(ability.get().id(), clockMillis.getAsLong())) + " §7Abklingzeit.";
        };
        if (feedback != null) {
            hud.flash(player, feedback);
        }
    }

    public static String slotName(int slot) {
        return switch (slot) {
            case 1 -> "Die Primärkraft";
            case 2 -> "Die Sekundärkraft";
            default -> "Die Ultimate";
        };
    }
}
