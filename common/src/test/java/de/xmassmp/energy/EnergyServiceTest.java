package de.xmassmp.energy;

import de.xmassmp.MutableClock;
import de.xmassmp.TestPlatform;
import de.xmassmp.TestPlatform.FakePlayer;
import de.xmassmp.XmasCore;
import de.xmassmp.config.XmasConfig;
import de.xmassmp.crystal.CrystalModels;
import de.xmassmp.crystal.Crystals;
import de.xmassmp.crystal.PristineStage;
import de.xmassmp.item.ItemSpec;
import de.xmassmp.player.PlayerData;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Energie aus Kills und Toden, Bruchstufen, Stärke, Anti-Farming. */
class EnergyServiceTest {

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-12-20T12:00:00Z"));
    private TestPlatform platform;
    private XmasCore core;
    private FakePlayer steve;
    private FakePlayer alex;

    @BeforeEach
    void setUp() {
        platform = new TestPlatform(dir);
        core = new XmasCore(platform, clock);
        core.start();
        steve = platform.join("Steve");
        alex = platform.join("Alex");
        core.players().get(steve.uuid()).setCrystalId("fire");
        core.players().get(alex.uuid()).setCrystalId("wind");
        core.onPlayerJoin(steve);
        core.onPlayerJoin(alex);
    }

    private int energy(FakePlayer player) {
        return core.players().get(player.uuid()).energy();
    }

    private void editConfig(String from, String to) throws IOException {
        Path config = dir.resolve("config.jsonc");
        String text = Files.readString(config);
        assertTrue(text.contains(from), from);
        Files.writeString(config, text.replace(from, to));
        core.reload();
    }

    @Test
    void killMovesEnergyFromVictimToKiller() {
        core.onPlayerDeath(alex, steve);
        assertEquals(6, energy(steve));
        assertEquals(4, energy(alex));
        assertTrue(steve.messages.get(0).contains("+1 Energie"));
        assertTrue(alex.messages.get(0).contains("-1 Energie"));
    }

    @Test
    void deathWithoutKillerOnlyCosts() {
        core.onPlayerDeath(steve, null);
        assertEquals(4, energy(steve));
        assertEquals(5, energy(alex));
    }

    @Test
    void energyStaysWithinBounds() {
        core.players().get(steve.uuid()).setEnergy(10);
        core.players().get(alex.uuid()).setEnergy(0);
        core.onPlayerDeath(alex, steve);
        assertEquals(10, energy(steve));
        assertEquals(0, energy(alex));
        assertTrue(steve.messages.get(0).contains("schon voll"));
        assertTrue(alex.messages.isEmpty());
    }

    @Test
    void sameVictimIsNotFarmed() {
        core.onPlayerDeath(alex, steve);
        core.onPlayerDeath(alex, steve);
        assertEquals(6, energy(steve));
        assertEquals(3, energy(alex), "der Besiegte verliert trotzdem");
        assertTrue(steve.messages.get(1).contains("erst vor kurzem"));

        clock.advance(Duration.ofMinutes(30));
        core.onPlayerDeath(alex, steve);
        assertEquals(7, energy(steve));
    }

    @Test
    void cooldownCanBeDisabled() throws IOException {
        editConfig("\"sameVictimCooldownMinutes\": 30", "\"sameVictimCooldownMinutes\": 0");
        core.onPlayerDeath(alex, steve);
        core.onPlayerDeath(alex, steve);
        assertEquals(7, energy(steve));
    }

    @Test
    void eventMultiplierScalesGain() throws IOException {
        Files.writeString(dir.resolve("events.jsonc"), """
                { "events": [ { "id": "double", "name": "Doppelte Energie", "enabled": false,
                    "dates": ["12-24"], "times": ["18:00"], "durationMinutes": 60,
                    "modifiers": { "energy_multiplier": 2.0 } } ] }
                """);
        core.reload();
        assertTrue(core.events().start("double"));
        core.onPlayerDeath(alex, steve);
        assertEquals(7, energy(steve));
    }

    @Test
    void stagesFollowConfiguredThresholds() {
        XmasConfig.Stages stages = new XmasConfig.Stages();
        assertEquals(PristineStage.BROKEN, EnergyService.stageFor(0, stages));
        assertEquals(PristineStage.DAMAGED, EnergyService.stageFor(1, stages));
        assertEquals(PristineStage.DAMAGED, EnergyService.stageFor(3, stages));
        assertEquals(PristineStage.SCRATCHED, EnergyService.stageFor(4, stages));
        assertEquals(PristineStage.PRISTINE, EnergyService.stageFor(7, stages));
        assertEquals(PristineStage.PRISTINE, EnergyService.stageFor(10, stages));
    }

    @Test
    void invalidStagesFallBackWithWarning() throws IOException {
        Path config = dir.resolve("config.jsonc");
        Files.writeString(config, Files.readString(config).replace(
                "\"stages\": { \"damaged\": 1, \"scratched\": 4, \"pristine\": 7 }",
                "\"stages\": { \"damaged\": 8, \"scratched\": 4, \"pristine\": 20 }"));
        List<String> messages = core.reload();
        assertTrue(messages.stream().anyMatch(m -> m.contains("energy.stages")), messages.toString());
        XmasConfig.Stages stages = core.config().get().energy.stages;
        assertEquals(List.of(1, 4, 7), List.of(stages.damaged, stages.scratched, stages.pristine));
    }

    @Test
    void stageChangeUpdatesItemAndNotifies() {
        List<String> changes = new ArrayList<>();
        core.energy().addStageListener((player, from, to) -> changes.add(player.name() + " " + from + "->" + to));
        core.players().get(alex.uuid()).setEnergy(4);
        core.crystalService().sync(alex);

        core.onPlayerDeath(alex, null);
        assertEquals(List.of("Alex SCRATCHED->DAMAGED"), changes);
        ItemSpec item = alex.crystals().get(0);
        assertEquals("damaged", item.data().get("stage"));
        assertEquals(CrystalModels.customModelData(Crystals.WIND, 1, PristineStage.DAMAGED), item.customModelData());
        assertTrue(alex.messages.stream().anyMatch(m -> m.contains("beschädigt")));
        core.tick();
        assertTrue(platform.effects.contains("s:minecraft:block.glass.break"));

        core.players().get(alex.uuid()).setEnergy(1);
        core.onPlayerDeath(alex, null);
        assertTrue(alex.messages.stream().anyMatch(m -> m.contains("zerbrochen")));
        assertEquals("broken", alex.crystals().get(0).data().get("stage"));
    }

    @Test
    void strengthScalesWithEnergyAndIsZeroWhenBroken() {
        PlayerData data = core.players().get(steve.uuid());
        data.setEnergy(0);
        assertEquals(0.0, core.energy().strength(data));
        data.setEnergy(1);
        assertEquals(0.6, core.energy().strength(data), 1e-9);
        data.setEnergy(10);
        assertEquals(1.0, core.energy().strength(data), 1e-9);
        data.setEnergy(4);
        assertEquals(0.6 + 0.4 * 3 / 9, core.energy().strength(data), 1e-9);
    }
}
