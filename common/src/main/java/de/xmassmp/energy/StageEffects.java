package de.xmassmp.energy;

import de.xmassmp.crystal.Crystal;
import de.xmassmp.crystal.CrystalRegistry;
import de.xmassmp.crystal.PristineStage;
import de.xmassmp.effect.DisplayHandle;
import de.xmassmp.effect.DisplaySpec;
import de.xmassmp.effect.DisplayTransform;
import de.xmassmp.effect.Easing;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.effect.EffectTimeline;
import de.xmassmp.effect.ParticleSpec;
import de.xmassmp.effect.Shapes;
import de.xmassmp.effect.SoundStack;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.player.PlayerDataStore;
import de.xmassmp.util.Rgb;
import de.xmassmp.util.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Animationen beim Wechsel der Bruchstufe, in der Farbe des Kristalls (der Grund-Sound kommt aus
 * {@link EnergyService}):
 * <ul>
 *   <li><b>Bruch</b> – Spannung baut sich in einem engen Ring auf, dann zerspringt der Kristall:
 *       Farbsplitter, leuchtende Kristallscherben (Item-Displays) fliegen auseinander und fallen;
 *       beim völligen Zerbrechen zusätzlich Rauch und ein dumpfer Nachhall.</li>
 *   <li><b>Reparatur</b> – Splitter sammeln sich spiralförmig, eine Scherbe setzt sich leuchtend
 *       zusammen, ein Lichtring breitet sich am Boden aus.</li>
 * </ul>
 */
public final class StageEffects implements EnergyService.StageListener {

    private static final String SHARD_ITEM = "minecraft:echo_shard";
    private static final Vec3 CHEST = new Vec3(0, 1.1, 0);
    private static final int SHARDS = 6;

    private final CrystalRegistry crystals;
    private final PlayerDataStore players;
    private final EffectEngine effects;

    public StageEffects(CrystalRegistry crystals, PlayerDataStore players, EffectEngine effects) {
        this.crystals = crystals;
        this.players = players;
        this.effects = effects;
    }

    @Override
    public void onStageChanged(PlayerHandle player, PristineStage from, PristineStage to) {
        Crystal crystal = crystalOf(player);
        Rgb color = crystal != null ? crystal.color() : Rgb.of(255, 255, 255);
        Rgb accent = crystal != null ? crystal.accentColor() : Rgb.of(160, 160, 160);
        EffectTimeline timeline = to.ordinal() < from.ordinal()
                ? shatter(color, accent, to == PristineStage.BROKEN)
                : restore(color, accent, to == PristineStage.PRISTINE);
        effects.play(timeline, player.world(), player::position);
    }

    private Crystal crystalOf(PlayerHandle player) {
        String id = players.get(player.uuid()).crystalId();
        return id == null ? null : crystals.get(id).orElse(null);
    }

    // ------------------------------------------------------------------ Bruch

    static EffectTimeline shatter(Rgb color, Rgb accent, boolean broken) {
        ParticleSpec tension = ParticleSpec.dust(color, 0.7f);
        ParticleSpec crackle = ParticleSpec.of("minecraft:electric_spark").count(2).spread(0.15).speed(0.05);
        ParticleSpec splinters = ParticleSpec.transition(color, accent, 1.1f).count(3).spread(0.12).speed(0.02);
        ParticleSpec sparks = ParticleSpec.of("minecraft:crit").count(18).spread(0.3).speed(0.6);
        ParticleSpec dust = ParticleSpec.transition(accent, color, 0.6f).count(2).spread(0.4, 0.1, 0.4);
        ParticleSpec smoke = ParticleSpec.of("minecraft:large_smoke").count(6).spread(0.3).speed(0.02);

        List<DisplayHandle> shards = new ArrayList<>();
        EffectTimeline.Builder b = EffectTimeline.builder();
        // 1. Spannung: enger werdender, flimmernder Ring um die Brust.
        b.phase(10, Easing.EASE_IN, (canvas, t, frame) -> {
            double radius = 0.9 - 0.6 * t;
            canvas.particles(tension, offset(Shapes.ring(radius, 14, frame * 0.4), CHEST));
            if (frame % 2 == 0) {
                canvas.particle(crackle, CHEST);
            }
        });
        // 2. Zerspringen: Splitter-Kugel, Funken und fliegende Scherben.
        int burst = b.cursor();
        b.at(burst, canvas -> {
            canvas.particles(splinters, offset(Shapes.sphere(0.45, 26), CHEST));
            canvas.particle(sparks, CHEST);
            for (int i = 0; i < SHARDS; i++) {
                float yaw = 360f * i / SHARDS;
                DisplaySpec spec = DisplaySpec.item(SHARD_ITEM).glow(color).fullBright()
                        .transform(DisplayTransform.scaled(0.45).withRotation(yaw, 35, 20));
                shards.add(canvas.display(spec, CHEST));
            }
        });
        b.sound(burst, SoundStack.builder()
                .layer("minecraft:block.amethyst_block.break", 1f, 0.9f, 0.1f, 0)
                .layer("minecraft:block.amethyst_block.resonate", 0.7f, broken ? 0.5f : 0.8f, 0.05f, 4)
                .build());
        // 3. Scherben fliegen auseinander, drehen sich und sinken; Farbstaub rieselt.
        b.during(burst + 1, 24, Easing.EASE_OUT, (canvas, t, frame) -> {
            if (frame == 0) {
                for (int i = 0; i < shards.size(); i++) {
                    double angle = 2 * Math.PI * i / shards.size();
                    Vec3 out = new Vec3(Math.cos(angle) * 1.4, -0.9, Math.sin(angle) * 1.4);
                    shards.get(i).moveTo(canvas.origin().add(CHEST).add(out), 22);
                    shards.get(i).transform(DisplayTransform.scaled(0.15)
                            .withRotation(360f * i / shards.size() + 180, 120, 90), 22);
                }
            }
            if (frame % 3 == 0) {
                canvas.particle(dust, CHEST.add(0, -0.4 * t, 0));
            }
            if (broken && frame % 4 == 0) {
                canvas.particle(smoke, CHEST);
            }
        });
        if (broken) {
            b.sound(burst + 8, SoundStack.builder()
                    .layer("minecraft:block.respawn_anchor.deplete", 0.8f, 0.7f, 0.05f, 0)
                    .build());
        }
        return b.build();
    }

    // ------------------------------------------------------------------ Reparatur

    static EffectTimeline restore(Rgb color, Rgb accent, boolean pristine) {
        ParticleSpec gather = ParticleSpec.transition(accent, color, 0.9f);
        ParticleSpec glitter = ParticleSpec.of("minecraft:end_rod").count(1).spread(0.05).speed(0.01);
        ParticleSpec ring = ParticleSpec.dust(color, 1.0f);
        ParticleSpec shine = ParticleSpec.of("minecraft:wax_off").count(10).spread(0.35).speed(0.4);

        DisplayHandle[] shard = new DisplayHandle[1];
        EffectTimeline.Builder b = EffectTimeline.builder();
        // 1. Splitter sammeln sich: Spirale zieht sich von außen zur Brust zusammen.
        b.phase(20, Easing.EASE_IN_OUT, (canvas, t, frame) -> {
            double radius = 1.3 - 1.1 * t;
            canvas.particles(gather, offset(Shapes.spiral(radius, 0.6, 1.5, 18, frame * 0.35),
                    CHEST.add(0, -0.3, 0)));
            if (frame % 3 == 0) {
                canvas.particle(glitter, CHEST);
            }
            if (frame == 6) {
                DisplaySpec spec = DisplaySpec.item(SHARD_ITEM).glow(color).fullBright()
                        .transform(DisplayTransform.scaled(0.05));
                shard[0] = canvas.display(spec, CHEST);
                shard[0].transform(DisplayTransform.scaled(0.7).withRotation(180, 0, 0), 14);
            }
        });
        // 2. Zusammengesetzt: Lichtring am Boden, Glanz, Scherbe dreht sich und verblasst.
        int done = b.cursor();
        b.sound(done, SoundStack.builder()
                .layer("minecraft:block.amethyst_block.resonate", 1f, 1.6f, 0.05f, 0)
                .layer(pristine ? "minecraft:entity.player.levelup" : "minecraft:block.amethyst_block.chime",
                        0.6f, pristine ? 1.5f : 1.2f, 0.05f, 3)
                .build());
        b.at(done, canvas -> canvas.particle(shine, CHEST));
        b.during(done, 14, Easing.EASE_OUT, (canvas, t, frame) -> {
            canvas.particles(ring, Shapes.ring(0.3 + 1.5 * t, 24, 0).stream().map(v -> v.add(0, 0.1, 0)).toList());
            if (frame == 0 && shard[0] != null) {
                shard[0].transform(DisplayTransform.scaled(0.3).withRotation(540, 0, 0), 12);
            }
        });
        return b.build();
    }

    private static List<Vec3> offset(List<Vec3> points, Vec3 by) {
        return points.stream().map(p -> p.add(by)).toList();
    }
}
