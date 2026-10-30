package de.xmassmp.bottle;

import de.xmassmp.config.XmasConfig;
import de.xmassmp.crystal.Crystal;
import de.xmassmp.crystal.CrystalRegistry;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.effect.EffectHandle;
import de.xmassmp.effect.EffectTimeline;
import de.xmassmp.effect.Easing;
import de.xmassmp.effect.ParticleSpec;
import de.xmassmp.effect.Shapes;
import de.xmassmp.effect.SoundStack;
import de.xmassmp.energy.EnergyLedger;
import de.xmassmp.energy.EnergyService;
import de.xmassmp.hud.HudService;
import de.xmassmp.item.CustomItem;
import de.xmassmp.item.ItemCatalog;
import de.xmassmp.item.ItemSpec;
import de.xmassmp.platform.Platform;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.player.PlayerData;
import de.xmassmp.player.PlayerDataStore;
import de.xmassmp.player.PlayerListener;
import de.xmassmp.text.Messages;
import de.xmassmp.util.Rgb;
import de.xmassmp.util.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Energy Bottles: abfüllen, trinken, zerlegen – exploit-sicher über das {@link BottleRegistry}.
 *
 * <p><b>Abfüllen:</b> Schleichen + Rechtsklick mit einer leeren Glasflasche gedrückt halten. Der Client
 * wiederholt die Nutzung etwa alle 4 Ticks; bleibt sie länger als {@link #RELEASE_TICKS} aus, gilt die
 * Taste als losgelassen. Die Ladezeit zählt der Server in eigenen Ticks, Makros oder Speed-Hacks
 * kürzen sie nicht ab. Am Ende wird alles erneut geprüft, dann passieren Glasflasche weg, Register-
 * Eintrag, Energie −1 und neue Flasche im selben Tick.
 *
 * <p><b>Trinken:</b> Register prüfen und schließen, Item entfernen, Energie +1 – in dieser Reihenfolge.
 * Kopien und Fälschungen werden entfernt und im Energie-Protokoll vermerkt.
 *
 * <p><b>Zerlegen:</b> {@link #canCraft} / {@link #consumeForCraft} für das Rezept Flasche → Fragments.
 */
public final class BottleService implements PlayerListener {

    public static final String GLASS_BOTTLE = "minecraft:glass_bottle";
    /** Ohne neues Nutzungs-Paket so lange gilt die Taste als losgelassen. */
    static final int RELEASE_TICKS = 6;
    /** Weiter darf man sich beim Abfüllen nicht bewegen (Blöcke, waagrecht). */
    static final double MAX_MOVE = 1.0;
    /** Erstes Zeichen des Ladebalkens im Ressourcenpaket (0 %), danach je 10 % ein Zeichen. */
    static final int BAR_GLYPH = 0xE030;
    private static final Rgb BOTTLE_COLOR = Rgb.hex("#5FE3D6");

    private static final class Charge {
        final long startTick;
        final int slot;
        final Vec3 start;
        long lastInputTick;
        EffectHandle effect;

        Charge(long startTick, int slot, Vec3 start) {
            this.startTick = startTick;
            this.slot = slot;
            this.start = start;
            this.lastInputTick = startTick;
        }
    }

    private final Platform platform;
    private final PlayerDataStore players;
    private final CrystalRegistry crystals;
    private final Supplier<XmasConfig> config;
    private final EnergyService energy;
    private final BottleRegistry registry;
    private final EffectEngine effects;
    private final HudService hud;
    private final LongSupplier clockMillis;
    private final XmasLogger logger;
    private final Map<UUID, Charge> charges = new HashMap<>();
    /** Letzter Schaden durch einen anderen Spieler (Kampf-Sperre fürs Abfüllen). */
    private final Map<UUID, Long> lastPvpHit = new HashMap<>();
    private final Map<UUID, Long> lastDrink = new HashMap<>();
    private long tick;

    public BottleService(Platform platform, PlayerDataStore players, CrystalRegistry crystals,
                         Supplier<XmasConfig> config, EnergyService energy, BottleRegistry registry,
                         EffectEngine effects, HudService hud, LongSupplier clockMillis, XmasLogger logger) {
        this.platform = platform;
        this.players = players;
        this.crystals = crystals;
        this.config = config;
        this.energy = energy;
        this.registry = registry;
        this.effects = effects;
        this.hud = hud;
        this.clockMillis = clockMillis;
        this.logger = logger;
    }

    public BottleRegistry registry() {
        return registry;
    }

    /** Neue, registrierte Flasche, z. B. für {@code /xmas give}. */
    public ItemSpec create(String origin, String creator) {
        return ItemCatalog.energyBottle(registry.create(origin, creator));
    }

    // ------------------------------------------------------------------ Abfüllen

    @Override
    public boolean onUseVanillaItem(PlayerHandle player, String itemId) {
        XmasConfig cfg = config.get();
        if (!GLASS_BOTTLE.equals(itemId) || !GLASS_BOTTLE.equals(player.mainHandItem()) || !player.isSneaking()
                || !cfg.energyBottle.enabled) {
            return false;
        }
        Charge charge = charges.get(player.uuid());
        if (charge != null) {
            charge.lastInputTick = tick;
            return true;
        }
        String problem = fillProblem(player, cfg);
        if (problem != null) {
            hud.flash(player, problem);
            return true;
        }
        charge = new Charge(tick, player.selectedSlot(), player.position());
        charge.effect = effects.play(chargeEffect(colorOf(player), chargeTicks(cfg)), player.world(), player::position);
        charges.put(player.uuid(), charge);
        return true;
    }

    /** Einmal pro Server-Tick. */
    public void tick(long ticks) {
        this.tick = ticks;
        if (charges.isEmpty()) {
            return;
        }
        XmasConfig cfg = config.get();
        int total = chargeTicks(cfg);
        for (UUID uuid : new ArrayList<>(charges.keySet())) {
            Charge charge = charges.get(uuid);
            PlayerHandle player = platform.player(uuid).orElse(null);
            if (player == null) {
                cancel(uuid, null, null);
                continue;
            }
            String abort = abortReason(player, charge, ticks);
            if (abort != null) {
                cancel(uuid, player, abort);
                continue;
            }
            long elapsed = ticks - charge.startTick;
            if (elapsed >= total) {
                charges.remove(uuid);
                finishFill(player, cfg);
                continue;
            }
            if (elapsed % 2 == 0) {
                int percent = (int) (elapsed * 100 / total);
                player.showTitle("", "§f" + Character.toString(BAR_GLYPH + percent / 10), 0, 6, 2);
            }
        }
    }

    private String abortReason(PlayerHandle player, Charge charge, long ticks) {
        if (ticks - charge.lastInputTick > RELEASE_TICKS) {
            return "";
        }
        if (!player.isAlive() || !player.isSneaking()) {
            return "§7Abfüllen abgebrochen.";
        }
        if (player.selectedSlot() != charge.slot || !GLASS_BOTTLE.equals(player.mainHandItem())) {
            return "§7Abfüllen abgebrochen: Flasche gewechselt.";
        }
        Vec3 moved = player.position().subtract(charge.start);
        if (Math.hypot(moved.x(), moved.z()) > MAX_MOVE) {
            return "§7Abfüllen abgebrochen: Du hast dich bewegt.";
        }
        return null;
    }

    private void finishFill(PlayerHandle player, XmasConfig cfg) {
        String problem = fillProblem(player, cfg);
        if (problem != null || !GLASS_BOTTLE.equals(player.mainHandItem())) {
            hud.flash(player, problem != null ? problem : "§7Abfüllen abgebrochen: keine Glasflasche.");
            player.showTitle("", "", 0, 1, 0);
            return;
        }
        if (!player.consumeMainHand(GLASS_BOTTLE, Map.of())) {
            hud.flash(player, "§7Abfüllen abgebrochen: keine leere Glasflasche.");
            return;
        }
        UUID id = registry.create("fill", player.name());
        int changed = energy.change(player, -1, EnergyLedger.BOTTLE_FILL, id.toString());
        if (changed != -1) {
            // Kann nach den Prüfungen nicht passieren; dann lieber nichts erzeugen.
            registry.close(id.toString(), player.name(), "fill_failed");
            player.giveItem(ItemSpec.vanilla(GLASS_BOTTLE));
            logger.warn("Energy Bottle für " + player.name() + " nicht erzeugt: Energie unverändert");
            return;
        }
        player.giveItem(ItemCatalog.energyBottle(id));
        player.showTitle("", "§b+1 Energy Bottle", 0, 20, 10);
        effects.play(fillDone(colorOf(player)), player.world(), player::position);
    }

    /** Grund, warum der Spieler gerade nicht abfüllen darf, oder {@code null}. */
    private String fillProblem(PlayerHandle player, XmasConfig cfg) {
        if (!cfg.energyBottle.enabled || !cfg.general.enabled) {
            return "§7Energy Bottles sind gerade abgeschaltet.";
        }
        if (player.isCreative()) {
            return "§7Im Kreativmodus kannst du keine Energie abfüllen.";
        }
        PlayerData data = players.get(player.uuid());
        if (crystals.get(data.crystalId()).isEmpty()) {
            return "§7Ohne Kristall gibt es keine Energie zum Abfüllen.";
        }
        if (data.energy() - 1 < cfg.energyBottle.keepAtLeast) {
            return "§cDu brauchst mindestens " + (cfg.energyBottle.keepAtLeast + 1) + " Energie zum Abfüllen.";
        }
        Long hit = lastPvpHit.get(player.uuid());
        long tagMillis = cfg.energyBottle.combatTagSeconds * 1000L;
        if (hit != null && clockMillis.getAsLong() - hit < tagMillis) {
            long rest = tagMillis - (clockMillis.getAsLong() - hit);
            return "§cIm Kampf kannst du nicht abfüllen §7(noch " + HudService.formatSeconds(rest) + ")";
        }
        return null;
    }

    private void cancel(UUID uuid, PlayerHandle player, String message) {
        Charge charge = charges.remove(uuid);
        if (charge != null && charge.effect != null) {
            charge.effect.cancel();
        }
        if (player != null) {
            player.showTitle("", "", 0, 1, 0);
            if (message != null && !message.isEmpty()) {
                hud.flash(player, message);
            }
        }
    }

    static int chargeTicks(XmasConfig cfg) {
        return cfg.energyBottle.chargeSeconds * 20;
    }

    // ------------------------------------------------------------------ Trinken

    @Override
    public boolean onUseItem(PlayerHandle player, Map<String, String> itemData) {
        if (!CustomItem.ENERGY_BOTTLE.id().equals(itemData.get(de.xmassmp.item.ItemSpec.DATA_TYPE))) {
            return false;
        }
        drink(player, itemData.get(ItemCatalog.DATA_BOTTLE));
        return true;
    }

    private void drink(PlayerHandle player, String id) {
        XmasConfig cfg = config.get();
        Map<String, String> match = id == null
                ? Map.of(de.xmassmp.item.ItemSpec.DATA_TYPE, CustomItem.ENERGY_BOTTLE.id())
                : Map.of(de.xmassmp.item.ItemSpec.DATA_TYPE, CustomItem.ENERGY_BOTTLE.id(), ItemCatalog.DATA_BOTTLE, id);
        BottleRegistry.State state = registry.state(id);
        if (state != BottleRegistry.State.ACTIVE) {
            player.consumeMainHand(CustomItem.ENERGY_BOTTLE.itemId(), match);
            reportInvalid(player, id, state);
            return;
        }
        PlayerData data = players.get(player.uuid());
        if (crystals.get(data.crystalId()).isEmpty()) {
            hud.flash(player, "§7Ohne Kristall kannst du keine Energie aufnehmen.");
            return;
        }
        if (data.energy() >= cfg.energy.max) {
            hud.flash(player, "§7Deine Energie ist schon voll.");
            return;
        }
        long now = clockMillis.getAsLong();
        Long last = lastDrink.get(player.uuid());
        if (last != null && now - last < cfg.energyBottle.drinkCooldownSeconds * 1000L) {
            return;
        }
        if (!registry.close(id, player.name(), "drink")) {
            reportInvalid(player, id, registry.state(id));
            return;
        }
        player.consumeMainHand(CustomItem.ENERGY_BOTTLE.itemId(), match);
        lastDrink.put(player.uuid(), now);
        energy.change(player, 1, EnergyLedger.BOTTLE_DRINK, id);
        player.giveItem(ItemSpec.vanilla(GLASS_BOTTLE));
        hud.flash(player, "§a+1 Energie §7(" + players.get(player.uuid()).energy() + "/" + cfg.energy.max + ")");
        effects.play(drinkEffect(colorOf(player)), player.world(), player::position);
        // Kopien derselben Flasche dürfen in keinem Werkbank-Feld mehr ein Ergebnis zeigen.
        platform.refreshCrafting();
    }

    private void reportInvalid(PlayerHandle player, String id, BottleRegistry.State state) {
        String what = state == BottleRegistry.State.USED ? "bereits verbraucht (Kopie)" : "unbekannt (Fälschung)";
        player.sendMessage(Messages.prefixed("§cDiese Energy Bottle ist ungültig und wurde entfernt."));
        int current = players.get(player.uuid()).energy();
        energy.ledger().record(player.uuid(), player.name(), current, current, EnergyLedger.SUSPICIOUS,
                (id == null ? "ohne-id" : id) + " " + what);
        logger.warn("Ungültige Energy Bottle bei " + player.name() + ": " + id + " " + what);
    }

    // ------------------------------------------------------------------ Zerlegen (Werkbank)

    /** Zeigt das Rezept Flasche → Fragments ein Ergebnis? Nur für aktive Flaschen. */
    public boolean canCraft(Map<String, String> bottleData) {
        return config.get().energyBottle.enabled
                && registry.state(bottleData.get(ItemCatalog.DATA_BOTTLE)) == BottleRegistry.State.ACTIVE;
    }

    /**
     * Schließt die Flasche beim Herausnehmen des Ergebnisses. Danach werden alle Werkbänke neu
     * berechnet, damit eine Kopie im selben Feld kein zweites Ergebnis liefert.
     *
     * @param crafter Spielername oder {@code crafter} (Crafter-Block)
     * @return {@code false}, wenn die Flasche nicht (mehr) aktiv war
     */
    public boolean consumeForCraft(Map<String, String> bottleData, String crafter) {
        boolean closed = registry.close(bottleData.get(ItemCatalog.DATA_BOTTLE), crafter, "craft");
        platform.refreshCrafting();
        return closed;
    }

    // ------------------------------------------------------------------ Sieger mit voller Energie

    /** Für {@link EnergyService#setOverflowHandler}: die Energie des Opfers fällt als Flasche zu Boden. */
    public void dropOverflow(PlayerHandle killer, PlayerHandle victim, int amount) {
        for (int i = 0; i < amount; i++) {
            victim.dropItem(create("overflow", killer.name() + " > " + victim.name()));
        }
        killer.sendMessage(Messages.prefixed("Deine Energie ist voll – die Energie von " + victim.name()
                + " liegt als §bEnergy Bottle§7 am Boden."));
    }

    // ------------------------------------------------------------------ Ereignisse

    @Override
    public void onDamaged(PlayerHandle victim, PlayerHandle attacker) {
        if (attacker != null && !attacker.uuid().equals(victim.uuid())) {
            lastPvpHit.put(victim.uuid(), clockMillis.getAsLong());
            lastPvpHit.put(attacker.uuid(), clockMillis.getAsLong());
        }
        if (charges.containsKey(victim.uuid())) {
            cancel(victim.uuid(), victim, "§cAbfüllen abgebrochen: Schaden genommen.");
        }
    }

    @Override
    public void onDeath(PlayerHandle victim, PlayerHandle killer) {
        cancel(victim.uuid(), victim, null);
    }

    @Override
    public void onQuit(UUID uuid) {
        cancel(uuid, null, null);
        lastDrink.remove(uuid);
    }

    /** Läuft gerade ein Abfüllvorgang? */
    public boolean isCharging(UUID uuid) {
        return charges.containsKey(uuid);
    }

    // ------------------------------------------------------------------ Effekte

    private Rgb colorOf(PlayerHandle player) {
        return crystals.get(players.get(player.uuid()).crystalId()).map(Crystal::color).orElse(BOTTLE_COLOR);
    }

    /** Energie fließt als Farbband von der Brust in die Flasche vor dem Spieler, Ton steigt mit. */
    static EffectTimeline chargeEffect(Rgb color, int ticks) {
        ParticleSpec stream = ParticleSpec.transition(color, BOTTLE_COLOR, 0.7f);
        ParticleSpec glint = ParticleSpec.of("minecraft:end_rod").speed(0.01);
        EffectTimeline.Builder b = EffectTimeline.builder();
        b.during(0, ticks, Easing.EASE_IN, (c, p, f) -> {
            Vec3 chest = new Vec3(0, 1.1, 0);
            double swirl = f * 0.5;
            Vec3 hand = new Vec3(Math.cos(swirl) * 0.45, 0.9 + 0.1 * Math.sin(swirl * 2), Math.sin(swirl) * 0.45);
            c.particles(stream, Shapes.line(chest, hand, 4 + (int) (p * 6)));
            if (f % 5 == 0) {
                c.particle(glint, hand);
            }
        });
        for (int t = 0; t < ticks; t += 10) {
            float pitch = 0.8f + 1.0f * t / ticks;
            b.sound(t, SoundStack.builder()
                    .layer("minecraft:block.amethyst_block.chime", 0.5f, pitch, 0.05f, 0)
                    .build());
        }
        return b.build();
    }

    static EffectTimeline fillDone(Rgb color) {
        ParticleSpec burst = ParticleSpec.transition(color, BOTTLE_COLOR, 1.1f).count(2).spread(0.1);
        ParticleSpec sparkle = ParticleSpec.of("minecraft:wax_on").count(6).spread(0.3).speed(0.5);
        return EffectTimeline.builder()
                .sound(0, SoundStack.builder()
                        .layer("minecraft:item.bottle.fill_dragonbreath", 1f, 1.2f, 0.1f, 0)
                        .layer("minecraft:block.amethyst_block.resonate", 0.8f, 1.6f, 0.1f, 2)
                        .build())
                .phase(10, Easing.EASE_OUT, (c, p, f) -> c.particles(burst,
                        Shapes.ring(0.2 + 0.7 * p, 16, f * 0.3).stream().map(v -> v.add(0, 1.0, 0)).toList()))
                .at(0, c -> c.particle(sparkle, new Vec3(0, 1.0, 0)))
                .build();
    }

    static EffectTimeline drinkEffect(Rgb color) {
        ParticleSpec rise = ParticleSpec.transition(BOTTLE_COLOR, color, 1.0f);
        ParticleSpec flash = ParticleSpec.of("minecraft:totem_of_undying").count(12).spread(0.3).speed(0.25);
        return EffectTimeline.builder()
                .sound(0, SoundStack.builder()
                        .layer("minecraft:entity.generic.drink", 0.8f, 1.1f, 0.1f, 0)
                        .layer("minecraft:block.amethyst_block.chime", 1f, 1.3f, 0.1f, 6)
                        .layer("minecraft:block.beacon.power_select", 0.4f, 1.9f, 0f, 14)
                        .build())
                .phase(16, Easing.EASE_IN_OUT, (c, p, f) -> c.particles(rise,
                        Shapes.spiral(0.7 - 0.3 * p, 0.2, 0.5, 6, f * 0.4).stream()
                                .map(v -> v.add(0, p * 1.9, 0)).toList()))
                .phase(1, (c, p, f) -> c.particle(flash, new Vec3(0, 1.2, 0)))
                .phase(8, Easing.EASE_OUT, (c, p, f) -> c.particles(ParticleSpec.dust(color, 0.9f),
                        Shapes.ring(0.4 + 1.2 * p, 20, 0).stream().map(v -> v.add(0, 0.1, 0)).toList()))
                .build();
    }

    /** Für Tests: aktive Abfüllvorgänge. */
    List<UUID> chargingPlayers() {
        return List.copyOf(charges.keySet());
    }
}
