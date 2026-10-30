package de.xmassmp.gameplay;

import de.xmassmp.config.XmasConfig;
import de.xmassmp.crystal.Crystal;
import de.xmassmp.crystal.CrystalModels;
import de.xmassmp.crystal.CrystalRegistry;
import de.xmassmp.effect.DisplayHandle;
import de.xmassmp.effect.DisplaySpec;
import de.xmassmp.effect.DisplayTransform;
import de.xmassmp.effect.Easing;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.effect.EffectTimeline;
import de.xmassmp.effect.ParticleSpec;
import de.xmassmp.effect.Shapes;
import de.xmassmp.effect.SoundStack;
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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Rechtsklick mit dem Upgrader: Kristall von Tier 1 auf Tier 2 (schaltet die Ultimate frei). */
public final class UpgraderService implements PlayerListener {

    private final CrystalRegistry crystals;
    private final PlayerDataStore players;
    private final Supplier<XmasConfig> config;
    private final EffectEngine effects;
    private final HudService hud;
    private final Consumer<UUID> changed;

    /** @param changed nach dem Upgrade (Item-Abgleich, Tab-Liste) */
    public UpgraderService(CrystalRegistry crystals, PlayerDataStore players, Supplier<XmasConfig> config,
                           EffectEngine effects, HudService hud, Consumer<UUID> changed) {
        this.crystals = crystals;
        this.players = players;
        this.config = config;
        this.effects = effects;
        this.hud = hud;
        this.changed = changed;
    }

    @Override
    public boolean onUseItem(PlayerHandle player, Map<String, String> itemData) {
        if (!CustomItem.UPGRADER.id().equals(itemData.get(ItemSpec.DATA_TYPE))) {
            return false;
        }
        if (!config.get().upgrader.enabled) {
            hud.flash(player, "§7Upgrader sind gerade abgeschaltet.");
            return true;
        }
        PlayerData data = players.get(player.uuid());
        Optional<Crystal> crystal = crystals.get(data.crystalId());
        if (crystal.isEmpty()) {
            hud.flash(player, "§7Du hast keinen Kristall zum Aufwerten.");
            return true;
        }
        if (data.tier() >= CrystalModels.MAX_TIER) {
            hud.flash(player, "§7Dein Kristall ist schon Tier " + CrystalModels.MAX_TIER + ".");
            return true;
        }
        if (!player.consumeMainHand(CustomItem.UPGRADER.itemId(), Map.of(ItemSpec.DATA_TYPE, CustomItem.UPGRADER.id()))) {
            return true;
        }
        data.setTier(CrystalModels.MAX_TIER);
        players.save(player.uuid());
        changed.accept(player.uuid());
        player.showTitle("§" + crystal.get().chatColor() + "Tier 2", "§7Deine Ultimate ist freigeschaltet", 5, 50, 15);
        player.sendMessage(Messages.prefixed("Dein §" + crystal.get().chatColor() + crystal.get().nameFallback()
                + "-Kristall§7 ist jetzt §eTier 2§7. Ultimate: Schleichen + Rechtsklick mit dem Kristall."));
        effects.play(upgradeEffect(crystal.get().color(), crystal.get().accentColor()), player.world(), player::position);
        return true;
    }

    /**
     * Aufladen: Säule in Kristallfarbe, der Kristall hebt sich und dreht immer schneller. Auslösen:
     * Lichtblitz, Totem-Partikel. Nachklingen: Ring am Boden und fallende Funken.
     */
    static EffectTimeline upgradeEffect(Rgb color, Rgb accent) {
        ParticleSpec column = ParticleSpec.transition(color, accent, 1.0f);
        ParticleSpec rise = ParticleSpec.of("minecraft:end_rod").speed(0.02);
        ParticleSpec flash = ParticleSpec.of("minecraft:flash");
        ParticleSpec totem = ParticleSpec.of("minecraft:totem_of_undying").count(40).spread(0.4).speed(0.6);
        ParticleSpec ring = ParticleSpec.dust(color, 1.3f);
        ParticleSpec sparks = ParticleSpec.of("minecraft:firework").count(2).spread(1.2, 0.2, 1.2).speed(0.02);
        DisplayHandle[] gem = new DisplayHandle[1];

        EffectTimeline.Builder b = EffectTimeline.builder();
        b.sound(0, SoundStack.builder()
                .layer("minecraft:block.beacon.activate", 1f, 1.2f, 0.05f, 0)
                .layer("minecraft:block.amethyst_block.resonate", 1f, 0.8f, 0.1f, 10)
                .build());
        b.phase(40, Easing.EASE_IN, (c, p, f) -> {
            if (f == 0) {
                gem[0] = c.display(DisplaySpec.item("minecraft:echo_shard").scale(0.6).glow(color).fullBright(),
                        new Vec3(0, 1.4, 0));
            }
            if (gem[0] != null) {
                gem[0].moveTo(c.origin().add(0, 1.4 + 0.9 * p, 0), 1);
                gem[0].transform(DisplayTransform.scaled(0.6 + 0.4 * p).withRotation((float) (f * f * 0.9), 0, 0), 1);
            }
            c.particles(column, Shapes.spiral(0.8 - 0.4 * p, 2.6, 2, 10, f * 0.35));
            if (f % 3 == 0) {
                c.particle(rise, new Vec3(0, 0.2, 0));
            }
        });
        b.sound(b.cursor(), SoundStack.builder()
                .layer("minecraft:item.totem.use", 0.6f, 1.4f, 0.05f, 0)
                .layer("minecraft:entity.player.levelup", 1f, 0.9f)
                .build());
        b.phase(1, (c, p, f) -> {
            c.particle(flash, new Vec3(0, 2.3, 0));
            c.particle(totem, new Vec3(0, 2.3, 0));
            if (gem[0] != null) {
                gem[0].remove();
            }
        });
        b.phase(24, Easing.EASE_OUT, (c, p, f) -> {
            c.particles(ring, Shapes.ring(0.4 + 2.4 * p, 28, f * 0.1).stream().map(v -> v.add(0, 0.1, 0)).toList());
            if (f % 4 == 0) {
                c.particle(sparks, new Vec3(0, 2.6, 0));
            }
        });
        return b.build();
    }
}
