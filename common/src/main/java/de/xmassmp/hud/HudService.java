package de.xmassmp.hud;

import de.xmassmp.ability.AbilityRegistry;
import de.xmassmp.ability.AbilityService;
import de.xmassmp.ability.ActiveAbility;
import de.xmassmp.config.XmasConfig;
import de.xmassmp.crystal.Crystal;
import de.xmassmp.crystal.CrystalRegistry;
import de.xmassmp.crystal.PristineStage;
import de.xmassmp.effect.Easing;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.effect.EffectTimeline;
import de.xmassmp.effect.ParticleSpec;
import de.xmassmp.effect.Shapes;
import de.xmassmp.effect.SoundStack;
import de.xmassmp.platform.Platform;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.player.PlayerData;
import de.xmassmp.player.PlayerDataStore;
import de.xmassmp.player.PlayerListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Actionbar mit den drei Fähigkeits-Icons (Primär, Sekundär, Ultimate) (Font-Zeichen aus dem Ressourcenpaket), Cooldowns und
 * Energie. Kurze Rückmeldungen ({@link #flash}) überdecken die Anzeige für einen Moment.
 *
 * <p>Endet ein Cooldown, gibt es einen leisen Ton und einen Partikel-Puls in Kristallfarbe.
 */
public final class HudService implements PlayerListener {

    /** So lange bleibt eine {@link #flash}-Meldung stehen. */
    public static final int FLASH_TICKS = 40;

    private record Flash(String text, long untilTick) {
    }

    private final Platform platform;
    private final CrystalRegistry crystals;
    private final AbilityRegistry abilities;
    private final PlayerDataStore players;
    private final Supplier<XmasConfig> config;
    private final Function<PlayerData, PristineStage> stage;
    private final EffectEngine effects;
    private final LongSupplier clockMillis;
    private final IntUnaryOperator requiredEnergy;
    /** Zusätze am Ende der Zeile, z. B. die Restzeit eines Totems; leer = nichts anhängen. */
    private final List<Function<UUID, String>> suffixes = new CopyOnWriteArrayList<>();
    private final Map<UUID, Flash> flashes = new HashMap<>();
    /** Fähigkeiten je Spieler, die beim letzten Tick noch im Cooldown waren. */
    private final Map<UUID, Set<String>> cooling = new HashMap<>();
    private long tick;

    public HudService(Platform platform, CrystalRegistry crystals, AbilityRegistry abilities, PlayerDataStore players,
                      Supplier<XmasConfig> config, Function<PlayerData, PristineStage> stage, EffectEngine effects,
                      LongSupplier clockMillis) {
        this(platform, crystals, abilities, players, config, stage, effects, clockMillis, slot -> 0);
    }

    /**
     * @param requiredEnergy Mindest-Energie je Slot (1–3), siehe {@code energy.access}
     */
    public HudService(Platform platform, CrystalRegistry crystals, AbilityRegistry abilities, PlayerDataStore players,
                      Supplier<XmasConfig> config, Function<PlayerData, PristineStage> stage, EffectEngine effects,
                      LongSupplier clockMillis, IntUnaryOperator requiredEnergy) {
        this.requiredEnergy = requiredEnergy;
        this.platform = platform;
        this.crystals = crystals;
        this.abilities = abilities;
        this.players = players;
        this.config = config;
        this.stage = stage;
        this.effects = effects;
        this.clockMillis = clockMillis;
    }

    /** Einmal pro Server-Tick. */
    public void tick(long ticks) {
        this.tick = ticks;
        XmasConfig cfg = config.get();
        boolean render = ticks % cfg.hud.updateTicks == 0;
        long now = clockMillis.getAsLong();
        for (PlayerHandle player : platform.onlinePlayers()) {
            PlayerData data = players.get(player.uuid());
            Optional<Crystal> crystal = crystals.get(data.crystalId());
            if (crystal.isEmpty()) {
                continue;
            }
            detectCooldownEnds(player, data, crystal.get(), now);
            Flash flash = flashes.get(player.uuid());
            if (flash != null && flash.untilTick() <= ticks) {
                flashes.remove(player.uuid());
                flash = null;
            }
            if (render && flash != null) {
                player.sendActionBar(flash.text());
            } else if (render && cfg.hud.enabled) {
                player.sendActionBar(line(data, crystal.get(), now));
            }
        }
    }

    /** Hängt einen Zusatz an die HUD-Zeile jedes Spielers an (leer = nichts). */
    public void addSuffix(Function<UUID, String> suffix) {
        suffixes.add(suffix);
    }

    /** Zeigt sofort eine kurze Meldung in der Actionbar, die das HUD für {@link #FLASH_TICKS} überdeckt. */
    public void flash(PlayerHandle player, String legacyText) {
        flashes.put(player.uuid(), new Flash(legacyText, tick + FLASH_TICKS));
        player.sendActionBar(legacyText);
    }

    @Override
    public void onQuit(UUID uuid) {
        flashes.remove(uuid);
        cooling.remove(uuid);
    }

    /** Eine HUD-Zeile, z. B. {@code [Icon1] ✔   [Icon2] 12s   [Icon3] T2   ✦ 7/10}. */
    String line(PlayerData data, Crystal crystal, long now) {
        boolean broken = stage.apply(data) == PristineStage.BROKEN;
        StringBuilder out = new StringBuilder();
        for (int slot = 1; slot <= Crystal.SLOTS; slot++) {
            out.append(slot(data, crystal, slot, broken, now)).append("§r   ");
        }
        int max = config.get().energy.max;
        out.append(broken ? "§c" : "§" + crystal.chatColor()).append("✦ ").append(data.energy()).append("/").append(max);
        for (Function<UUID, String> suffix : suffixes) {
            String extra = suffix.apply(data.uuid());
            if (extra != null && !extra.isEmpty()) {
                out.append("§r   ").append(extra);
            }
        }
        return out.toString();
    }

    private String slot(PlayerData data, Crystal crystal, int slot, boolean broken, long now) {
        String icon = crystal.hudIcon(slot);
        Optional<ActiveAbility> ability = abilities.active(crystal.id(), slot);
        if (ability.isEmpty()) {
            return "§8" + icon + " –";
        }
        int tier = AbilityService.requiredTier(ability.get());
        if (data.tier() < tier) {
            return "§8" + icon + " T" + tier;
        }
        if (broken) {
            return "§8" + icon + " §c✖";
        }
        int needed = requiredEnergy.applyAsInt(slot);
        if (data.energy() < needed) {
            return "§8" + icon + " §c✦" + needed;
        }
        long remaining = data.remainingCooldownMillis(ability.get().id(), now);
        if (remaining > 0) {
            return "§8" + icon + " §7" + formatSeconds(remaining);
        }
        return "§f" + icon + " §a✔";
    }

    /** Aufgerundete Sekunden, ab einer Minute als {@code m:ss}. */
    public static String formatSeconds(long millis) {
        long seconds = (millis + 999) / 1000;
        return seconds < 60 ? seconds + "s" : String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private void detectCooldownEnds(PlayerHandle player, PlayerData data, Crystal crystal, long now) {
        Set<String> previous = cooling.computeIfAbsent(player.uuid(), uuid -> new HashSet<>());
        for (int slot = 1; slot <= Crystal.SLOTS; slot++) {
            Optional<ActiveAbility> ability = abilities.active(crystal.id(), slot);
            if (ability.isEmpty()) {
                continue;
            }
            String id = ability.get().id();
            if (data.isOnCooldown(id, now)) {
                previous.add(id);
            } else if (previous.remove(id)) {
                effects.play(readyPulse(crystal), player.world(), player::position);
            }
        }
    }

    /** Leiser Ton und ein Ring, der sich um die Füße ausbreitet. */
    static EffectTimeline readyPulse(Crystal crystal) {
        ParticleSpec dust = ParticleSpec.transition(crystal.accentColor(), crystal.color(), 0.9f);
        return EffectTimeline.builder()
                .sound(0, SoundStack.builder()
                        .layer("minecraft:block.note_block.chime", 0.5f, 1.7f, 0.05f, 0)
                        .layer("minecraft:block.amethyst_block.step", 0.6f, 1.4f)
                        .build())
                .phase(8, Easing.EASE_OUT, (c, p, f) -> c.particles(dust,
                        Shapes.ring(0.3 + 0.9 * p, 14, f * 0.4).stream().map(v -> v.add(0, 0.1, 0)).toList()))
                .build();
    }
}
