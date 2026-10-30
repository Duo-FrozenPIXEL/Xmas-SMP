package de.xmassmp.gameplay;

import de.xmassmp.config.XmasConfig;
import de.xmassmp.crystal.Crystal;
import de.xmassmp.crystal.CrystalModels;
import de.xmassmp.crystal.CrystalRegistry;
import de.xmassmp.effect.Easing;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.effect.EffectTimeline;
import de.xmassmp.effect.ParticleSpec;
import de.xmassmp.effect.Shapes;
import de.xmassmp.effect.SoundStack;
import de.xmassmp.gui.GuiButton;
import de.xmassmp.gui.GuiHost;
import de.xmassmp.gui.GuiView;
import de.xmassmp.hud.HudService;
import de.xmassmp.item.CustomItem;
import de.xmassmp.item.ItemSpec;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.player.PlayerData;
import de.xmassmp.player.PlayerDataStore;
import de.xmassmp.player.PlayerListener;
import de.xmassmp.text.Messages;
import de.xmassmp.util.Rgb;
import de.xmassmp.util.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Gem Trader: Rechtsklick öffnet eine GUI mit freier Wahl des neuen Kristalls. Der Trader wird erst
 * bei der Auswahl verbraucht; Tier und Energie bleiben laut Config erhalten, Cooldowns werden
 * zurückgesetzt.
 */
public final class TraderService implements PlayerListener {

    private final CrystalRegistry crystals;
    private final PlayerDataStore players;
    private final Supplier<XmasConfig> config;
    private final GuiHost gui;
    private final EffectEngine effects;
    private final HudService hud;
    private final LongSupplier clockMillis;
    private final Consumer<UUID> changed;
    private final Map<UUID, Long> lastTrade = new HashMap<>();

    public TraderService(CrystalRegistry crystals, PlayerDataStore players, Supplier<XmasConfig> config,
                         GuiHost gui, EffectEngine effects, HudService hud, LongSupplier clockMillis,
                         Consumer<UUID> changed) {
        this.crystals = crystals;
        this.players = players;
        this.config = config;
        this.gui = gui;
        this.effects = effects;
        this.hud = hud;
        this.clockMillis = clockMillis;
        this.changed = changed;
    }

    @Override
    public boolean onUseItem(PlayerHandle player, Map<String, String> itemData) {
        if (!CustomItem.GEM_TRADER.id().equals(itemData.get(ItemSpec.DATA_TYPE))) {
            return false;
        }
        String problem = problem(player);
        if (problem != null) {
            hud.flash(player, problem);
            return true;
        }
        gui.open(player.uuid(), view(player));
        return true;
    }

    private String problem(PlayerHandle player) {
        XmasConfig cfg = config.get();
        if (!cfg.trader.enabled) {
            return "§7Der Kristall-Händler ist gerade abgeschaltet.";
        }
        if (crystals.get(players.get(player.uuid()).crystalId()).isEmpty()) {
            return "§7Du hast keinen Kristall zum Tauschen.";
        }
        Long last = lastTrade.get(player.uuid());
        long wait = cfg.trader.cooldownMinutes * 60_000L;
        if (last != null && clockMillis.getAsLong() - last < wait) {
            return "§7Nächster Tausch in §f" + HudService.formatSeconds(wait - (clockMillis.getAsLong() - last));
        }
        return null;
    }

    /** Fünf Kristalle in der Mitte der zweiten Reihe, der eigene ausgegraut. */
    GuiView view(PlayerHandle player) {
        PlayerData data = players.get(player.uuid());
        XmasConfig cfg = config.get();
        GuiView.Builder b = GuiView.builder("§8Kristall-Händler › §fWähle deinen Kristall", 3);
        List<Crystal> all = crystals.all().stream().toList();
        int start = Math.max(0, (GuiView.COLUMNS - all.size()) / 2);
        for (int i = 0; i < all.size() && start + i < GuiView.COLUMNS; i++) {
            Crystal crystal = all.get(i);
            String name = "§" + crystal.chatColor() + crystal.nameFallback() + "-Kristall";
            if (crystal.id().equals(data.crystalId())) {
                b.set(1, start + i, GuiButton.label("minecraft:gray_dye", "§8" + crystal.nameFallback() + "-Kristall",
                        "§7Das ist dein aktueller Kristall."));
            } else if (!cfg.crystal(crystal.id()).enabled) {
                b.set(1, start + i, GuiButton.label("minecraft:barrier", "§8" + crystal.nameFallback() + "-Kristall",
                        "§7Gerade abgeschaltet."));
            } else {
                b.set(1, start + i, GuiButton.action("minecraft:echo_shard", name, c -> choose(player, crystal),
                        "§7Klick: tauschen", "§8Der Händler wird verbraucht.",
                        cfg.trader.keepTier ? "§8Tier bleibt erhalten." : "§8Tier wird auf 1 gesetzt.",
                        cfg.trader.keepEnergy ? "§8Energie bleibt erhalten." : "§8Energie wird zurückgesetzt.")
                        .withGlow(true));
            }
        }
        b.set(2, 4, GuiButton.action("minecraft:barrier", "§7Abbrechen", c -> gui.close(player.uuid()),
                "§8Nichts wird verbraucht."));
        return b.build();
    }

    void choose(PlayerHandle player, Crystal target) {
        gui.close(player.uuid());
        String problem = problem(player);
        if (problem != null) {
            hud.flash(player, problem);
            return;
        }
        XmasConfig cfg = config.get();
        PlayerData data = players.get(player.uuid());
        Optional<Crystal> before = crystals.get(data.crystalId());
        if (before.isEmpty() || before.get().id().equals(target.id()) || !cfg.crystal(target.id()).enabled) {
            return;
        }
        if (!player.consumeMainHand(CustomItem.GEM_TRADER.itemId(),
                Map.of(ItemSpec.DATA_TYPE, CustomItem.GEM_TRADER.id()))) {
            hud.flash(player, "§7Halte den Kristall-Händler in der Haupthand.");
            return;
        }
        data.setCrystalId(target.id());
        data.clearCooldowns();
        if (!cfg.trader.keepTier) {
            data.setTier(CrystalModels.MIN_TIER);
        }
        if (!cfg.trader.keepEnergy) {
            data.setEnergy(cfg.energy.start);
        }
        players.save(player.uuid());
        lastTrade.put(player.uuid(), clockMillis.getAsLong());
        changed.accept(player.uuid());
        player.showTitle("§" + target.chatColor() + target.nameFallback(), "§7Dein neuer Kristall", 5, 40, 15);
        player.sendMessage(Messages.prefixed("Du hast den §" + before.get().chatColor() + before.get().nameFallback()
                + "§7- gegen den §" + target.chatColor() + target.nameFallback() + "-Kristall§7 getauscht."));
        effects.play(swapEffect(before.get().color(), target.color(), target.accentColor()), player.world(),
                player::position);
    }

    /**
     * Der alte Kristall löst sich in einem Wirbel seiner Farbe auf, der Wirbel wechselt die Farbe und
     * zieht sich zusammen, der neue Kristall setzt sich mit einem Funkenring zusammen.
     */
    static EffectTimeline swapEffect(Rgb from, Rgb to, Rgb accent) {
        ParticleSpec smoke = ParticleSpec.of("minecraft:witch").count(2).spread(0.2);
        ParticleSpec burst = ParticleSpec.transition(to, accent, 1.4f).count(2).spread(0.1);
        ParticleSpec sparks = ParticleSpec.of("minecraft:electric_spark").count(10).spread(0.4).speed(0.3);
        return EffectTimeline.builder()
                .sound(0, SoundStack.builder()
                        .layer("minecraft:block.amethyst_cluster.break", 1f, 0.8f, 0.1f, 0)
                        .layer("minecraft:entity.illusioner.mirror_move", 0.8f, 1.2f, 0.1f, 4)
                        .build())
                .phase(30, Easing.EASE_IN_OUT, (c, p, f) -> {
                    ParticleSpec swirl = ParticleSpec.transition(from.lerp(to, p), to.lerp(accent, p), 1.0f);
                    c.particles(swirl, Shapes.spiral(1.2 - 0.9 * p, 2.0, 2, 14, f * 0.45).stream()
                            .map(v -> v.add(0, 0.1, 0)).toList());
                    if (f % 4 == 0) {
                        c.particle(smoke, new Vec3(0, 1.2, 0));
                    }
                })
                .sound(30, SoundStack.builder()
                        .layer("minecraft:block.amethyst_block.resonate", 1f, 1.5f, 0.05f, 0)
                        .layer("minecraft:block.beacon.power_select", 0.6f, 1.6f)
                        .build())
                .phase(1, (c, p, f) -> {
                    c.particles(burst, Shapes.sphere(0.6, 30).stream().map(v -> v.add(0, 1.2, 0)).toList());
                    c.particle(sparks, new Vec3(0, 1.2, 0));
                })
                .phase(14, Easing.EASE_OUT, (c, p, f) -> c.particles(ParticleSpec.dust(to, 1.0f),
                        Shapes.ring(0.3 + 1.6 * p, 22, 0).stream().map(v -> v.add(0, 0.1, 0)).toList()))
                .build();
    }
}
