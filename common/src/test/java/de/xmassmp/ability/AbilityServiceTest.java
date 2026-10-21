package de.xmassmp.ability;

import de.xmassmp.config.XmasConfig;
import de.xmassmp.crystal.Crystals;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.effect.EffectQuality;
import de.xmassmp.effect.EffectSink;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.platform.WorldRef;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.player.PlayerData;
import de.xmassmp.util.Vec3;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbilityServiceTest {

    private final XmasConfig config = new XmasConfig();
    private final AtomicLong now = new AtomicLong(1_000_000);
    private double eventMultiplier = 1.0;
    private final AtomicInteger activations = new AtomicInteger();

    private final EffectEngine effects = new EffectEngine(new EffectSink() {
        @Override
        public void particle(WorldRef world, Vec3 position, de.xmassmp.effect.ParticleSpec spec, int count) {
        }

        @Override
        public void sound(WorldRef world, Vec3 position, String sound, float volume, float pitch) {
        }
    }, () -> EffectQuality.HIGH, () -> 1000, new Random(), XmasLogger.NOOP);

    private final AbilityService service = new AbilityService(() -> config, effects, now::get, () -> eventMultiplier);

    private final ActiveAbility meteor = new ActiveAbility() {
        @Override
        public String id() {
            return "fire.meteor";
        }

        @Override
        public String crystalId() {
            return "fire";
        }

        @Override
        public int requiredTier() {
            return 2;
        }

        @Override
        public Duration cooldown() {
            return Duration.ofSeconds(60);
        }

        @Override
        public int slot() {
            return 2;
        }

        @Override
        public void activate(AbilityContext context) {
            activations.incrementAndGet();
        }
    };

    private final PlayerHandle player = new PlayerHandle() {
        @Override
        public UUID uuid() {
            return UUID.randomUUID();
        }

        @Override
        public String name() {
            return "Tester";
        }

        @Override
        public WorldRef world() {
            return new WorldRef("overworld");
        }

        @Override
        public Vec3 position() {
            return Vec3.ZERO;
        }

        @Override
        public Vec3 eyePosition() {
            return Vec3.UP;
        }

        @Override
        public Vec3 lookDirection() {
            return new Vec3(1, 0, 0);
        }

        @Override
        public void sendActionBar(String legacyText) {
        }
    };

    private PlayerData fireTier(int tier) {
        PlayerData data = new PlayerData(UUID.randomUUID(), 5);
        data.setCrystalId("fire");
        data.setTier(tier);
        return data;
    }

    @Test
    void lockedBelowRequiredTier() {
        assertEquals(AbilityService.Result.LOCKED, service.tryActivate(player, fireTier(1), Crystals.FIRE, meteor));
        assertEquals(0, activations.get());
    }

    @Test
    void cooldownBlocksUntilExpired() {
        PlayerData data = fireTier(2);
        assertEquals(AbilityService.Result.ACTIVATED, service.tryActivate(player, data, Crystals.FIRE, meteor));
        now.addAndGet(59_999);
        assertEquals(AbilityService.Result.ON_COOLDOWN, service.tryActivate(player, data, Crystals.FIRE, meteor));
        now.addAndGet(1);
        assertEquals(AbilityService.Result.ACTIVATED, service.tryActivate(player, data, Crystals.FIRE, meteor));
        assertEquals(2, activations.get());
    }

    @Test
    void configAndEventMultipliersCombine() {
        XmasConfig.CrystalSettings fire = new XmasConfig.CrystalSettings();
        fire.cooldownMultiplier = 2.0;
        config.crystals.put("fire", fire);
        eventMultiplier = 0.25;
        PlayerData data = fireTier(2);
        service.tryActivate(player, data, Crystals.FIRE, meteor);
        assertEquals(30_000, data.remainingCooldownMillis("fire.meteor", now.get()));
    }

    @Test
    void switchesDisableAbilities() {
        PlayerData data = fireTier(2);
        config.general.abilitiesEnabled = false;
        assertEquals(AbilityService.Result.DISABLED, service.tryActivate(player, data, Crystals.FIRE, meteor));
        config.general.abilitiesEnabled = true;

        XmasConfig.CrystalSettings fire = new XmasConfig.CrystalSettings();
        fire.enabled = false;
        config.crystals.put("fire", fire);
        assertEquals(AbilityService.Result.DISABLED, service.tryActivate(player, data, Crystals.FIRE, meteor));
        config.crystals.clear();

        data.setCrystalId("wind");
        assertEquals(AbilityService.Result.DISABLED, service.tryActivate(player, data, Crystals.FIRE, meteor),
                "fremder Kristall");
        assertEquals(0, activations.get());
    }

    @Test
    void registryFindsSlotsAndRejectsDuplicates() {
        AbilityRegistry registry = new AbilityRegistry();
        registry.register(meteor);
        assertEquals(meteor, registry.active("fire", 2).orElseThrow());
        assertEquals(1, registry.forCrystal("fire").size());
        assertEquals(0, registry.forCrystal("wind").size());
        assertThrows(IllegalArgumentException.class, () -> registry.register(meteor));
    }
}
