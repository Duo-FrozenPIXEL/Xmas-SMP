package de.xmassmp.ability;

import de.xmassmp.crystal.Crystal;
import de.xmassmp.effect.Easing;
import de.xmassmp.effect.EffectTimeline;
import de.xmassmp.effect.ParticleSpec;
import de.xmassmp.effect.Shapes;
import de.xmassmp.effect.SoundStack;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.util.Rgb;
import de.xmassmp.util.Vec3;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * Platzhalter, bis die echten Kräfte aus {@code docs/kraefte-entwurf.md} freigegeben sind: zeigt nur
 * Partikel in der Kristallfarbe, ohne Schaden oder Wirkung. Cooldown, Energie und Tier laufen wie bei
 * einer echten Kraft, damit HUD und Steuerung testbar sind.
 * <ul>
 *   <li>Slot 1 – Ring breitet sich am Boden aus.</li>
 *   <li>Slot 2 – Strahl in Blickrichtung.</li>
 *   <li>Slot 3 (Ultimate) – Spirale steigt auf, dann platzt eine Kugel.</li>
 * </ul>
 */
public record PlaceholderAbility(Crystal crystal, int slot) implements ActiveAbility {

    private static final Vec3 CHEST = new Vec3(0, 1.1, 0);

    /** Je Kristall drei Platzhalter (Slot 1–3). */
    public static void registerAll(AbilityRegistry registry, Collection<Crystal> crystals) {
        for (Crystal crystal : crystals) {
            for (int slot = 1; slot <= Crystal.SLOTS; slot++) {
                registry.register(new PlaceholderAbility(crystal, slot));
            }
        }
    }

    @Override
    public String id() {
        return crystal.id() + ".placeholder_" + slot;
    }

    @Override
    public String crystalId() {
        return crystal.id();
    }

    @Override
    public int requiredTier() {
        return slot == 3 ? 2 : 1;
    }

    @Override
    public Duration cooldown() {
        return switch (slot) {
            case 1 -> Duration.ofSeconds(8);
            case 2 -> Duration.ofSeconds(12);
            default -> Duration.ofSeconds(45);
        };
    }

    @Override
    public void activate(AbilityContext context) {
        PlayerHandle player = context.player();
        EffectTimeline timeline = switch (slot) {
            case 1 -> ring(crystal.color(), crystal.accentColor());
            case 2 -> beam(crystal.color(), crystal.accentColor(), player.lookDirection());
            default -> ultimate(crystal.color(), crystal.accentColor());
        };
        context.effects().play(timeline, player.world(), player.position());
    }

    static EffectTimeline ring(Rgb color, Rgb accent) {
        ParticleSpec dust = ParticleSpec.transition(color, accent, 1.2f);
        return EffectTimeline.builder()
                .sound(0, SoundStack.builder().layer("minecraft:block.amethyst_block.chime", 1f, 1.4f, 0.05f, 0).build())
                .phase(12, Easing.EASE_OUT, (canvas, t, frame) -> canvas.particles(dust,
                        offset(Shapes.ring(0.4 + 2.6 * t, 28, frame * 0.2), new Vec3(0, 0.15, 0))))
                .build();
    }

    static EffectTimeline beam(Rgb color, Rgb accent, Vec3 look) {
        ParticleSpec dust = ParticleSpec.dust(color, 1.0f);
        ParticleSpec spark = ParticleSpec.dust(accent, 0.7f).spread(0.08, 0.08, 0.08).count(2);
        Vec3 dir = look.length() < 1e-6 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 from = CHEST.add(0, 0.4, 0).add(dir.scale(0.6));
        return EffectTimeline.builder()
                .sound(0, SoundStack.builder().layer("minecraft:entity.breeze.shoot", 0.8f, 1.2f, 0.05f, 0).build())
                .phase(8, Easing.LINEAR, (canvas, t, frame) -> {
                    Vec3 to = from.add(dir.scale(1 + 9 * t));
                    canvas.particles(dust, Shapes.line(from, to, 6 + (int) (24 * t)));
                    canvas.particle(spark, to);
                })
                .build();
    }

    static EffectTimeline ultimate(Rgb color, Rgb accent) {
        ParticleSpec rise = ParticleSpec.transition(accent, color, 1.0f);
        ParticleSpec burst = ParticleSpec.dust(color, 1.4f);
        ParticleSpec flash = ParticleSpec.of("minecraft:end_rod").count(20).spread(0.3, 0.3, 0.3).speed(0.25);
        EffectTimeline.Builder b = EffectTimeline.builder();
        b.sound(0, SoundStack.builder().layer("minecraft:block.beacon.activate", 1f, 1.3f, 0.05f, 0).build());
        b.phase(20, Easing.EASE_IN, (canvas, t, frame) -> canvas.particles(rise,
                Shapes.spiral(1.2 - 0.8 * t, 2.2, 1.5, 20, frame * 0.4)));
        int pop = b.cursor();
        b.sound(pop, SoundStack.builder()
                .layer("minecraft:entity.generic.explode", 0.6f, 1.4f, 0.05f, 0)
                .layer("minecraft:block.amethyst_block.resonate", 1f, 1.0f, 0.05f, 2)
                .build());
        b.at(pop, canvas -> canvas.particle(flash, CHEST));
        b.during(pop, 10, Easing.EASE_OUT, (canvas, t, frame) -> canvas.particles(burst,
                offset(Shapes.sphere(0.5 + 3 * t, 40), CHEST)));
        return b.build();
    }

    private static List<Vec3> offset(List<Vec3> points, Vec3 by) {
        return points.stream().map(p -> p.add(by)).toList();
    }
}
