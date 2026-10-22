package de.xmassmp.player;

import de.xmassmp.MutableClock;
import de.xmassmp.TestPlatform;
import de.xmassmp.TestPlatform.FakePlayer;
import de.xmassmp.XmasCore;
import de.xmassmp.platform.PlayerHandle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Weiterleitung der Spieler-Ereignisse vom Adapter an die {@link PlayerListener}. */
class PlayerEventsTest {

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-12-20T12:00:00Z"));
    private final List<String> trace = new ArrayList<>();
    private TestPlatform platform;
    private XmasCore core;

    @BeforeEach
    void setUp() {
        platform = new TestPlatform(dir);
        core = new XmasCore(platform, clock);
        core.start();
        core.addPlayerListener(new PlayerListener() {
            @Override
            public void onJoin(PlayerHandle player) {
                trace.add("join " + player.name());
            }

            @Override
            public void onQuit(UUID uuid) {
                trace.add("quit");
            }

            @Override
            public void onDeath(PlayerHandle victim, PlayerHandle killer) {
                trace.add("death " + victim.name() + " by " + (killer == null ? "-" : killer.name()));
            }

            @Override
            public void onRespawn(PlayerHandle player) {
                trace.add("respawn " + player.name());
            }

            @Override
            public boolean onSwapHands(PlayerHandle player) {
                trace.add("swap1");
                return player.isSneaking();
            }

            @Override
            public boolean onUseItem(PlayerHandle player, Map<String, String> itemData) {
                trace.add("use " + itemData.get("type"));
                return true;
            }
        });
    }

    @Test
    void eventsReachListeners() {
        FakePlayer steve = platform.join("Steve");
        FakePlayer alex = platform.join("Alex");
        core.onPlayerJoin(steve);
        core.onPlayerDeath(steve, alex);
        core.onPlayerDeath(alex, null);
        core.onPlayerRespawn(steve);
        assertTrue(core.onUseItem(steve, Map.of("type", "energy_bottle")));
        core.onPlayerQuit(steve.uuid());
        assertEquals(List.of("join Steve", "death Steve by Alex", "death Alex by -", "respawn Steve",
                "use energy_bottle", "quit"), trace);
    }

    @Test
    void selfKillCountsAsNoKiller() {
        FakePlayer steve = platform.join("Steve");
        core.onPlayerDeath(steve, steve);
        assertEquals(List.of("death Steve by -"), trace);
    }

    @Test
    void firstHandlingListenerWins() {
        List<String> second = new ArrayList<>();
        core.addPlayerListener(new PlayerListener() {
            @Override
            public boolean onSwapHands(PlayerHandle player) {
                second.add("swap2");
                return true;
            }
        });
        FakePlayer steve = platform.join("Steve");
        steve.sneaking = true;
        assertTrue(core.onSwapHands(steve));
        assertTrue(second.isEmpty());

        steve.sneaking = false;
        assertTrue(core.onSwapHands(steve));
        assertEquals(List.of("swap2"), second);
    }

    @Test
    void failingListenerDoesNotBreakOthers() {
        List<String> late = new ArrayList<>();
        core.addPlayerListener(new PlayerListener() {
            @Override
            public void onRespawn(PlayerHandle player) {
                throw new IllegalStateException("kaputt");
            }
        });
        core.addPlayerListener(new PlayerListener() {
            @Override
            public void onRespawn(PlayerHandle player) {
                late.add("ok");
            }
        });
        core.onPlayerRespawn(platform.join("Steve"));
        assertEquals(List.of("ok"), late);
        assertEquals(1, platform.errors.size());
    }

    @Test
    void disabledPluginOnlyHandlesQuit() throws IOException {
        Path config = dir.resolve("config.jsonc");
        Files.writeString(config, Files.readString(config).replaceFirst("\"enabled\": true", "\"enabled\": false"));
        core.reload();
        FakePlayer steve = platform.join("Steve");
        core.onPlayerJoin(steve);
        core.onPlayerDeath(steve, null);
        assertFalse(core.onSwapHands(steve));
        assertFalse(core.onUseItem(steve, Map.of("type", "upgrader")));
        core.onPlayerQuit(steve.uuid());
        assertEquals(List.of("quit"), trace);
    }
}
