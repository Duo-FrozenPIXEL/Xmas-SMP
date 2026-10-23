package de.xmassmp.fabric;

import de.xmassmp.effect.DisplayHandle;
import de.xmassmp.effect.DisplaySpec;
import de.xmassmp.effect.EffectSink;
import de.xmassmp.effect.ParticleSpec;
import de.xmassmp.platform.WorldRef;
import de.xmassmp.util.Vec3;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Sendet Partikel, Sounds und Displays der {@link de.xmassmp.effect.EffectEngine} an die Clients. */
final class FabricEffectSink implements EffectSink {

    private final FabricPlatform platform;
    private final Logger logger;
    private final Map<String, ParticleOptions> simpleTypes = new HashMap<>();
    private final Set<String> warned = new HashSet<>();

    FabricEffectSink(FabricPlatform platform, Logger logger) {
        this.platform = platform;
        this.logger = logger;
    }

    @Override
    public void particle(WorldRef world, Vec3 position, ParticleSpec spec, int count) {
        ServerLevel level = platform.level(world);
        ParticleOptions options = options(spec);
        if (level == null || options == null) {
            return;
        }
        Vec3 spread = spec.spread();
        level.sendParticles(options, position.x(), position.y(), position.z(), count,
                spread.x(), spread.y(), spread.z(), spec.speed());
    }

    @Override
    public void sound(WorldRef world, Vec3 position, String sound, float volume, float pitch) {
        ServerLevel level = platform.level(world);
        if (level == null) {
            return;
        }
        level.playSound(null, position.x(), position.y(), position.z(),
                SoundEvent.createVariableRangeEvent(McIds.parse(sound)), SoundSource.PLAYERS, volume, pitch);
    }

    @Override
    public DisplayHandle spawnDisplay(WorldRef world, Vec3 position, DisplaySpec spec) {
        ServerLevel level = platform.level(world);
        return level == null ? DisplayHandle.none() : new FabricDisplay(level, position, spec);
    }

    private ParticleOptions options(ParticleSpec spec) {
        switch (spec.type()) {
            case ParticleSpec.DUST:
                return new DustParticleOptions(spec.color().value(), spec.size());
            case ParticleSpec.DUST_TRANSITION:
                return new DustColorTransitionOptions(spec.color().value(), spec.toColor().value(), spec.size());
            default:
                return simpleTypes.computeIfAbsent(spec.type(), this::lookupSimple);
        }
    }

    private ParticleOptions lookupSimple(String id) {
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getValue(McIds.parse(id));
        if (type instanceof SimpleParticleType simple) {
            return simple;
        }
        if (warned.add(id)) {
            logger.warn("Partikel {} unbekannt oder braucht Zusatzdaten – wird ignoriert", id);
        }
        return null;
    }
}
