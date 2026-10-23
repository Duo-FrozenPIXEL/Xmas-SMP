package de.xmassmp.hud;

import de.xmassmp.MutableClock;
import de.xmassmp.TestPlatform;
import de.xmassmp.TestPlatform.FakePlayer;
import de.xmassmp.XmasCore;
import de.xmassmp.ability.AbilityContext;
import de.xmassmp.ability.ActiveAbility;
import de.xmassmp.crystal.Crystals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** F-Taste, Rückmeldungen, Actionbar-HUD und Cooldown-Ende. */
class HudAndControlsTest {

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-12-20T12:00:00Z"));
    private final List<String> fired = new ArrayList<>();
    private TestPlatform platform;
    private XmasCore core;
    private FakePlayer steve;

    private ActiveAbility ability(String id, int slot, int tier, int cooldownSeconds) {
        return new ActiveAbility() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String crystalId() {
                return "fire";
            }

            @Override
            public int requiredTier() {
                return tier;
            }

            @Override
            public Duration cooldown() {
                return Duration.ofSeconds(cooldownSeconds);
            }

            @Override
            public int slot() {
                return slot;
            }

            @Override
            public void activate(AbilityContext context) {
                fired.add(id + "@" + context.strength());
            }
        };
    }

    @BeforeEach
    void setUp() {
        platform = new TestPlatform(dir);
        core = new XmasCore(platform, clock);
        core.abilities().register(ability("fire.flame_burst", 1, 1, 10));
        core.abilities().register(ability("fire.meteor", 2, 2, 90));
        core.start();
        steve = platform.join("Steve");
        core.players().get(steve.uuid()).setCrystalId("fire");
        core.onPlayerJoin(steve);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            core.tick();
        }
    }

    @Test
    void swapHandsFiresSlotOneSneakingSlotTwo() {
        assertTrue(core.onSwapHands(steve));
        assertEquals(1, fired.size());
        assertTrue(fired.get(0).startsWith("fire.flame_burst@"));

        steve.sneaking = true;
        assertTrue(core.onSwapHands(steve));
        assertEquals(1, fired.size(), "Tier 2 fehlt");
        assertTrue(steve.lastActionBar().contains("Tier 2"));

        core.players().get(steve.uuid()).setTier(2);
        assertTrue(core.onSwapHands(steve));
        assertEquals("fire.meteor", fired.get(1).split("@")[0]);
    }

    @Test
    void cooldownFeedbackAndHudLine() {
        core.onSwapHands(steve);
        clock.advance(Duration.ofMillis(2500));
        core.onSwapHands(steve);
        assertEquals("§7Noch §f8s §7Abklingzeit.", steve.lastActionBar());

        // Meldung bleibt 2 s stehen, danach das HUD
        ticks(HudService.FLASH_TICKS + 10);
        String hud = steve.lastActionBar();
        String icon1 = Crystals.FIRE.hudIcon(1);
        String icon2 = Crystals.FIRE.hudIcon(2);
        assertEquals("§8" + icon1 + " §78s§r   §8" + icon2 + " T2§r   §c✦ 5/10", hud);
    }

    @Test
    void readyAbilityShowsCheckAndEnergyInCrystalColor() {
        ticks(10);
        assertEquals("§f" + Crystals.FIRE.hudIcon(1) + " §a✔§r   §8" + Crystals.FIRE.hudIcon(2) + " T2§r   §c✦ 5/10",
                steve.lastActionBar());
        core.players().get(steve.uuid()).setCrystalId("nature");
        ticks(10);
        assertTrue(steve.lastActionBar().endsWith("§a✦ 5/10"));
        assertTrue(steve.lastActionBar().contains(Crystals.NATURE.hudIcon(1) + " –"), "Natur hat noch keine Fähigkeiten");
        assertFalse(core.onSwapHands(steve), "ohne Kräfte tauscht F normal");
    }

    @Test
    void missingSlotIsInterceptedWithMessage() {
        core.players().get(steve.uuid()).setTier(2);
        core.abilities().register(new ActiveAbility() {
            @Override
            public String id() {
                return "wind.gust";
            }

            @Override
            public String crystalId() {
                return "wind";
            }

            @Override
            public int requiredTier() {
                return 1;
            }

            @Override
            public int slot() {
                return 1;
            }

            @Override
            public void activate(AbilityContext context) {
            }
        });
        core.players().get(steve.uuid()).setCrystalId("wind");
        steve.sneaking = true;
        assertTrue(core.onSwapHands(steve));
        assertTrue(steve.lastActionBar().contains("gibt es noch nicht"));
    }

    @Test
    void brokenCrystalBlocksWithMessage() {
        core.players().get(steve.uuid()).setEnergy(0);
        assertTrue(core.onSwapHands(steve));
        assertTrue(fired.isEmpty());
        assertTrue(steve.lastActionBar().contains("zerbrochen"));
        ticks(HudService.FLASH_TICKS + 10);
        assertTrue(steve.lastActionBar().contains("§c✖"));
    }

    @Test
    void cooldownEndPlaysPulseOnce() {
        core.onSwapHands(steve);
        ticks(1);
        platform.effects.clear();
        clock.advance(Duration.ofSeconds(10));
        ticks(2);
        assertEquals(1, platform.effects.stream().filter(e -> e.equals("s:minecraft:block.note_block.chime")).count());
        assertTrue(platform.effects.contains("p:minecraft:dust_color_transition"));
        ticks(20);
        assertEquals(1, platform.effects.stream().filter(e -> e.equals("s:minecraft:block.note_block.chime")).count());
    }

    @Test
    void playersWithoutCrystalKeepNormalSwapAndNoHud() throws IOException {
        Path config = dir.resolve("config.jsonc");
        Files.writeString(config, Files.readString(config).replace("\"giveOnJoin\": true", "\"giveOnJoin\": false"));
        core.reload();
        FakePlayer alex = platform.join("Alex");
        core.onPlayerJoin(alex);
        assertFalse(core.onSwapHands(alex));
        ticks(20);
        assertTrue(alex.actionBar.isEmpty());
    }

    @Test
    void switchesDisableControlsAndHud() throws IOException {
        Path config = dir.resolve("config.jsonc");
        String text = Files.readString(config)
                .replace("\"swapHandsActivates\": true", "\"swapHandsActivates\": false")
                .replace("\"enabled\": true,\n    // Aktualisierung", "\"enabled\": false,\n    // Aktualisierung");
        Files.writeString(config, text);
        core.reload();
        assertFalse(core.onSwapHands(steve));
        steve.actionBar.clear();
        ticks(20);
        assertTrue(steve.actionBar.isEmpty());
    }

    @Test
    void formatSeconds() {
        assertEquals("1s", HudService.formatSeconds(1));
        assertEquals("10s", HudService.formatSeconds(10_000));
        assertEquals("1:30", HudService.formatSeconds(89_001));
    }
}
