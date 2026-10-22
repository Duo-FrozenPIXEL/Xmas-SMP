package de.xmassmp.crystal;

import de.xmassmp.config.XmasConfig;
import de.xmassmp.effect.Easing;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.effect.EffectTimeline;
import de.xmassmp.effect.ParticleSpec;
import de.xmassmp.effect.Shapes;
import de.xmassmp.effect.SoundStack;
import de.xmassmp.item.ItemCatalog;
import de.xmassmp.item.ItemSpec;
import de.xmassmp.platform.Platform;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.player.PlayerData;
import de.xmassmp.player.PlayerDataStore;
import de.xmassmp.player.PlayerListener;
import de.xmassmp.text.Messages;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Hält das Kristall-Item jedes Spielers passend zu seinen {@link PlayerData}: Die Daten sind die
 * Wahrheit, das Item ist nur ihre Anzeige. Dadurch hat jeder Spieler genau einen Kristall, und
 * Kopien (Behälter, Item-Rahmen) bleiben wirkungslos.
 */
public final class CrystalService implements PlayerListener {

    /** Abstand der regelmäßigen Abgleiche in Server-Ticks. */
    public static final int SYNC_INTERVAL_TICKS = 20;
    /** Gedroppte Kristalle in diesem Umkreis verschwinden beim Abgleich. */
    static final double DROP_SWEEP_RADIUS = 8;

    private final Platform platform;
    private final CrystalRegistry crystals;
    private final PlayerDataStore players;
    private final Supplier<XmasConfig> config;
    private final Function<PlayerData, PristineStage> stage;
    private final EffectEngine effects;
    private final RandomGenerator random;

    /**
     * @param stage Bruchstufe eines Spielers (aus seiner Energie)
     */
    public CrystalService(Platform platform, CrystalRegistry crystals, PlayerDataStore players,
                          Supplier<XmasConfig> config, Function<PlayerData, PristineStage> stage,
                          EffectEngine effects, RandomGenerator random) {
        this.platform = platform;
        this.crystals = crystals;
        this.players = players;
        this.config = config;
        this.stage = stage;
        this.effects = effects;
        this.random = random;
    }

    /** Einmal pro Server-Tick. */
    public void tick(long ticks) {
        if (ticks % SYNC_INTERVAL_TICKS == 0) {
            platform.onlinePlayers().forEach(this::sync);
        }
    }

    @Override
    public void onJoin(PlayerHandle player) {
        assignIfMissing(player);
        sync(player);
    }

    @Override
    public void onRespawn(PlayerHandle player) {
        sync(player);
    }

    @Override
    public void onDeath(PlayerHandle victim, PlayerHandle killer) {
        // Der Kristall ist beim Tod schon gedroppt; er erscheint beim Respawn neu im Inventar.
        if (config.get().crystalItem.soulbound) {
            victim.removeDroppedCrystals(DROP_SWEEP_RADIUS);
        }
    }

    /** Bringt das Kristall-Item eines Spielers sofort auf den aktuellen Stand. */
    public void sync(PlayerHandle player) {
        boolean soulbound = config.get().crystalItem.soulbound;
        if (soulbound) {
            player.removeDroppedCrystals(DROP_SWEEP_RADIUS);
        }
        player.syncCrystalItem(expectedItem(player.uuid()).orElse(null), soulbound);
    }

    /** Sofortiger Abgleich, falls der Spieler online ist (z. B. nach Admin-Änderungen). */
    public void sync(UUID uuid) {
        platform.player(uuid).ifPresent(this::sync);
    }

    /** Das Item, das der Spieler gerade haben sollte; leer, wenn er keinen gültigen Kristall hat. */
    public Optional<ItemSpec> expectedItem(UUID uuid) {
        PlayerData data = players.get(uuid);
        Optional<Crystal> crystal = crystals.get(data.crystalId());
        if (crystal.isEmpty()) {
            return Optional.empty();
        }
        XmasConfig cfg = config.get();
        int tier = Math.max(CrystalModels.MIN_TIER, Math.min(CrystalModels.MAX_TIER, data.tier()));
        return Optional.of(ItemCatalog.ownedCrystal(crystal.get(), tier, stage.apply(data), data.energy(),
                cfg.energy.max, uuid, cfg.crystalItem.soulbound));
    }

    /**
     * Gibt einem Spieler ohne (gültigen) Kristall einen zufälligen aktivierten, falls
     * {@code crystalItem.giveOnJoin} an ist.
     *
     * @return der neue Kristall
     */
    public Optional<Crystal> assignIfMissing(PlayerHandle player) {
        XmasConfig cfg = config.get();
        PlayerData data = players.get(player.uuid());
        if (!cfg.crystalItem.giveOnJoin || crystals.get(data.crystalId()).isPresent()) {
            return Optional.empty();
        }
        List<Crystal> enabled = crystals.all().stream().filter(c -> cfg.crystal(c.id()).enabled).toList();
        Optional<Crystal> chosen = CrystalRegistry.random(enabled, random);
        chosen.ifPresent(crystal -> {
            data.setCrystalId(crystal.id());
            players.save(player.uuid());
            if (!cfg.crystalItem.soulbound) {
                // Ohne Seelenbindung legt der Abgleich nichts nach – einmalig geben.
                expectedItem(player.uuid()).ifPresent(player::giveItem);
            }
            player.sendMessage(Messages.prefixed("Du hast den §" + crystal.chatColor() + crystal.nameFallback()
                    + "-Kristall§7 erhalten. Er ist an dich gebunden."));
            effects.play(receiveEffect(crystal), player.world(), player::position);
        });
        return chosen;
    }

    /** Kurze Spirale in Kristallfarbe, die um den Spieler aufsteigt und oben aufblüht. */
    static EffectTimeline receiveEffect(Crystal crystal) {
        ParticleSpec trail = ParticleSpec.transition(crystal.color(), crystal.accentColor(), 1.2f);
        ParticleSpec burst = ParticleSpec.dust(crystal.color(), 1.6f).count(3).spread(0.15);
        return EffectTimeline.builder()
                .sound(0, SoundStack.builder()
                        .layer("minecraft:block.amethyst_block.resonate", 1f, 1.2f, 0.1f, 0)
                        .layer("minecraft:block.amethyst_block.chime", 1f, 1.5f, 0.2f, 8)
                        .build())
                .phase(24, Easing.EASE_IN_OUT, (c, p, f) -> c.particles(trail,
                        Shapes.spiral(0.9 - 0.4 * p, 0.25, 0.5, 6, f * 0.35 + p * 2.4).stream()
                                .map(v -> v.add(0, p * 2.1, 0)).toList()))
                .sound(24, SoundStack.builder().layer("minecraft:entity.player.levelup", 0.6f, 1.6f).build())
                .phase(1, (c, p, f) -> c.particles(burst, Shapes.sphere(0.6, 24).stream()
                        .map(v -> v.add(0, 2.2, 0)).toList()))
                .build();
    }
}
