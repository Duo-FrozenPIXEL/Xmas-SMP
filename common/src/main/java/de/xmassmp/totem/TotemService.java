package de.xmassmp.totem;

import de.xmassmp.config.XmasConfig;
import de.xmassmp.effect.DisplayHandle;
import de.xmassmp.effect.DisplaySpec;
import de.xmassmp.effect.DisplayTransform;
import de.xmassmp.effect.Easing;
import de.xmassmp.effect.EffectEngine;
import de.xmassmp.effect.EffectHandle;
import de.xmassmp.effect.EffectTimeline;
import de.xmassmp.effect.ParticleSpec;
import de.xmassmp.effect.Shapes;
import de.xmassmp.effect.SoundStack;
import de.xmassmp.energy.EnergyLedger;
import de.xmassmp.energy.EnergyService;
import de.xmassmp.hud.HudService;
import de.xmassmp.item.CustomItem;
import de.xmassmp.item.ItemSpec;
import de.xmassmp.platform.Platform;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.platform.WorldRef;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.player.PlayerDataStore;
import de.xmassmp.player.PlayerListener;
import de.xmassmp.text.Messages;
import de.xmassmp.util.BlockPos;
import de.xmassmp.util.JsonFiles;
import de.xmassmp.util.Rgb;
import de.xmassmp.util.Vec3;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Repair Kit als Totem (Regeln: docs/implementierungsplan.md, 1.6).
 *
 * <ul>
 *   <li>Aufstellen: global höchstens ein Totem; die Koordinaten gehen per Chat und Titel an alle.</li>
 *   <li>Teilnehmer ist, wer in den ersten {@code totem.joinSeconds} im Kreis steht. Wer ihn danach
 *       verlässt, stirbt oder sich ausloggt, scheidet aus. Ein Server-Neustart zählt nicht: Teilnehmer
 *       haben danach {@link #RECONNECT_GRACE_TICKS}, um zurückzukommen.</li>
 *   <li>Nach {@code totem.durationMinutes} zerbricht das Totem, erst dann bekommen alle verbliebenen
 *       Teilnehmer mit weniger als {@code totem.restoreTo} Energie genau diesen Wert.</li>
 *   <li>Zerstören dauert fest {@code totem.breakSeconds} (der Adapter liefert den Abbau-Fortschritt);
 *       loslassen, sterben oder wegschauen setzt den Countdown zurück. Dann scheitert das Ritual für alle,
 *       das Repair Kit ist verloren.</li>
 * </ul>
 */
public final class TotemService implements PlayerListener {

    static final int TICKS_PER_SECOND = 20;
    /** Nach einem Neustart so lange dürfen Teilnehmer offline sein. */
    static final int RECONNECT_GRACE_TICKS = 60 * TICKS_PER_SECOND;
    /** Ankündigungen an alle bei dieser Restzeit in Sekunden. */
    private static final int[] ANNOUNCE_AT = {300, 60, 30, 10};
    /** Höchstens so oft geht „Totem wird angegriffen!“ in den Chat. */
    private static final int ATTACK_BROADCAST_TICKS = 10 * TICKS_PER_SECOND;
    private static final Rgb RING = Rgb.hex("#7CFFB2");
    private static final Rgb RING_ACCENT = Rgb.hex("#FF5FD7");
    private static final Rgb DANGER = Rgb.hex("#FF2D55");
    private static final Rgb DANGER_ACCENT = Rgb.hex("#8E3BFF");

    /** Laufendes Totem. Wird in {@code totem.json} gespeichert. */
    static final class Active {
        String world;
        int x;
        int y;
        int z;
        String placedBy;
        long ageTicks;
        long totalTicks;
        Set<UUID> participants = new LinkedHashSet<>();
        Set<UUID> eliminated = new LinkedHashSet<>();

        WorldRef worldRef() {
            return new WorldRef(world);
        }

        BlockPos pos() {
            return new BlockPos(x, y, z);
        }
    }

    private final Platform platform;
    private final PlayerDataStore players;
    private final Supplier<XmasConfig> config;
    private final EnergyService energy;
    private final EffectEngine effects;
    private final HudService hud;
    private final XmasLogger logger;
    private final Path file;
    private Active active;
    private EffectHandle ambience;
    /** Angreifer → Tick, an dem sein Abbauen begann. */
    private final Map<UUID, Long> attackers = new HashMap<>();
    private long reconnectUntilAge = -1;
    private long lastAttackBroadcast = Long.MIN_VALUE / 2;
    private long tick;

    public TotemService(Platform platform, PlayerDataStore players, Supplier<XmasConfig> config, EnergyService energy,
                        EffectEngine effects, HudService hud, XmasLogger logger) {
        this.platform = platform;
        this.players = players;
        this.config = config;
        this.energy = energy;
        this.effects = effects;
        this.hud = hud;
        this.logger = logger;
        this.file = platform.dataDirectory().resolve("totem.json");
        hud.addSuffix(this::hudSuffix);
    }

    // ------------------------------------------------------------------ Abfragen

    public boolean isActive() {
        return active != null;
    }

    public Optional<String> status() {
        if (active == null) {
            return Optional.empty();
        }
        long left = Math.max(0, active.totalTicks - active.ageTicks);
        return Optional.of("Totem bei " + where(active) + " · noch " + HudService.formatSeconds(left * 50)
                + " · " + remaining(active) + " Teilnehmer · aufgestellt von " + active.placedBy);
    }

    // ------------------------------------------------------------------ Aufstellen

    /**
     * Rechtsklick mit dem Repair Kit auf einen Block.
     *
     * @param pos Stelle für das Totem (über dem angeklickten Block)
     * @return {@code true}, wenn der Klick behandelt wurde
     */
    public boolean tryPlace(PlayerHandle player, WorldRef world, BlockPos pos) {
        XmasConfig cfg = config.get();
        if (!CustomItem.REPAIR_KIT.itemId().equals(player.mainHandItem())) {
            return false;
        }
        if (!cfg.totem.enabled) {
            hud.flash(player, "§7Repair Kits sind gerade abgeschaltet.");
            return true;
        }
        if (active != null) {
            hud.flash(player, "§cEs läuft schon ein Totem bei " + where(active) + ".");
            return true;
        }
        if (!platform.totemHost().place(world, pos)) {
            hud.flash(player, "§7Hier kann das Totem nicht stehen.");
            return true;
        }
        if (!player.consumeMainHand(CustomItem.REPAIR_KIT.itemId(),
                Map.of(ItemSpec.DATA_TYPE, CustomItem.REPAIR_KIT.id()))) {
            platform.totemHost().remove(world, pos);
            return true;
        }
        Active totem = new Active();
        totem.world = world.id();
        totem.x = pos.x();
        totem.y = pos.y();
        totem.z = pos.z();
        totem.placedBy = player.name();
        totem.totalTicks = cfg.totem.durationMinutes * 60L * TICKS_PER_SECOND;
        active = totem;
        attackers.clear();
        reconnectUntilAge = -1;
        save();
        startAmbience();
        effects.play(placeEffect(), world, pos.topCenter().add(0, -1, 0));
        String coords = where(totem);
        for (PlayerHandle online : platform.onlinePlayers()) {
            if (cfg.totem.announceCoordinates) {
                online.showTitle("§d✚ Repair-Totem", "§f" + coords, 10, 70, 20);
            }
        }
        broadcast("§d" + player.name() + "§7 hat ein §dRepair-Totem§7 aufgestellt"
                + (cfg.totem.announceCoordinates ? " bei §f" + coords : "") + "§7. Wer in den ersten "
                + cfg.totem.joinSeconds + " s im Kreis (" + cfg.totem.radius + " Blöcke) steht und "
                + cfg.totem.durationMinutes + " Minuten bleibt, bekommt " + cfg.totem.restoreTo + " Energie.");
        return true;
    }

    // ------------------------------------------------------------------ Ablauf

    /** Einmal pro Server-Tick. */
    public void tick(long ticks) {
        this.tick = ticks;
        Active totem = active;
        if (totem == null) {
            return;
        }
        XmasConfig cfg = config.get();
        totem.ageTicks++;
        if (totem.ageTicks % TICKS_PER_SECOND == 0) {
            if (!platform.totemHost().isPresent(totem.worldRef(), totem.pos())) {
                fail(null, "§7Das Repair-Totem bei §f" + where(totem) + "§7 ist verschwunden.");
                return;
            }
            updateMembers(totem, cfg);
            announceRemaining(totem);
            save();
        }
        tickAttack(totem, cfg);
        if (totem.ageTicks >= totem.totalTicks) {
            complete(totem, cfg);
        }
    }

    private void updateMembers(Active totem, XmasConfig cfg) {
        boolean joining = totem.ageTicks <= cfg.totem.joinSeconds * (long) TICKS_PER_SECOND;
        for (PlayerHandle player : platform.onlinePlayers()) {
            UUID uuid = player.uuid();
            boolean inside = player.isAlive() && isInside(totem, player, cfg);
            if (joining && inside && !totem.eliminated.contains(uuid) && totem.participants.add(uuid)) {
                player.sendMessage(Messages.prefixed("§aDu nimmst am Repair-Totem teil.§7 Bleib bis zum Ende im Kreis!"));
            } else if (!inside && totem.participants.contains(uuid) && !joining) {
                eliminate(totem, uuid, player, "§cDu hast den Kreis verlassen – das Totem wirkt für dich nicht mehr.");
            }
        }
        if (reconnectUntilAge < 0 || totem.ageTicks > reconnectUntilAge) {
            for (UUID uuid : Set.copyOf(totem.participants)) {
                if (platform.player(uuid).isEmpty()) {
                    eliminate(totem, uuid, null, null);
                }
            }
        }
    }

    static boolean isInside(Active totem, PlayerHandle player, XmasConfig cfg) {
        if (!player.world().id().equals(totem.world)) {
            return false;
        }
        Vec3 center = totem.pos().center();
        Vec3 d = player.position().subtract(center);
        return Math.hypot(d.x(), d.z()) <= cfg.totem.radius && Math.abs(d.y()) <= cfg.totem.radius;
    }

    private void eliminate(Active totem, UUID uuid, PlayerHandle player, String message) {
        if (!totem.participants.remove(uuid)) {
            return;
        }
        totem.eliminated.add(uuid);
        if (player != null && message != null) {
            player.sendMessage(Messages.prefixed(message));
            effects.play(EffectTimeline.builder()
                    .sound(0, SoundStack.builder().layer("minecraft:block.beacon.deactivate", 0.6f, 1.6f).build())
                    .build(), player.world(), player.position());
        }
    }

    private void announceRemaining(Active totem) {
        long left = (totem.totalTicks - totem.ageTicks) / TICKS_PER_SECOND;
        for (int at : ANNOUNCE_AT) {
            if (left == at) {
                broadcast("§dRepair-Totem§7 bei §f" + where(totem) + "§7: noch §f" + HudService.formatSeconds(at * 1000L)
                        + "§7, " + remaining(totem) + " Teilnehmer.");
            }
        }
    }

    private void complete(Active totem, XmasConfig cfg) {
        int restored = 0;
        for (UUID uuid : Set.copyOf(totem.participants)) {
            Optional<PlayerHandle> player = platform.player(uuid);
            if (player.isEmpty() || !isInside(totem, player.get(), cfg)) {
                continue;
            }
            int current = players.get(uuid).energy();
            if (current < cfg.totem.restoreTo) {
                energy.change(player.get(), cfg.totem.restoreTo - current, EnergyLedger.TOTEM, totem.placedBy);
                player.get().showTitle("§a✚ " + cfg.totem.restoreTo + " Energie", "§7Das Totem hat dich geheilt", 5, 50, 15);
                restored++;
            } else {
                player.get().sendMessage(Messages.prefixed("Du hattest schon genug Energie – das Totem gab dir nichts."));
            }
        }
        platform.totemHost().remove(totem.worldRef(), totem.pos());
        effects.play(shatterEffect(RING, RING_ACCENT, cfg.totem.radius), totem.worldRef(), totem.pos().center());
        broadcast("§dDas Repair-Totem§7 bei §f" + where(totem) + "§7 ist zerfallen. §a" + restored
                + "§7 Spieler bekamen ihre Energie zurück.");
        clear();
    }

    private void fail(String breaker, String message) {
        Active totem = active;
        if (totem == null) {
            return;
        }
        platform.totemHost().remove(totem.worldRef(), totem.pos());
        effects.play(shatterEffect(DANGER, DANGER_ACCENT, config.get().totem.radius), totem.worldRef(), totem.pos().center());
        broadcast(message);
        logger.info("Repair-Totem bei " + where(totem) + " gescheitert" + (breaker != null ? " (zerstört von " + breaker + ")" : ""));
        clear();
    }

    private void clear() {
        active = null;
        attackers.clear();
        if (ambience != null) {
            ambience.cancel();
            ambience = null;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            logger.error("totem.json konnte nicht gelöscht werden", e);
        }
    }

    // ------------------------------------------------------------------ Zerstören

    /**
     * Abbau-Fortschritt vom Adapter: {@code stage} 0–9 während des Abbauens, {@code -1} beim Abbruch.
     *
     * @return {@code true}, wenn {@code pos} das laufende Totem ist
     */
    public boolean onMiningProgress(PlayerHandle player, WorldRef world, BlockPos pos, int stage) {
        Active totem = active;
        if (totem == null || !totem.world.equals(world.id()) || !totem.pos().equals(pos)) {
            return false;
        }
        if (stage < 0) {
            if (attackers.remove(player.uuid()) != null) {
                effects.play(EffectTimeline.builder().sound(0, SoundStack.builder()
                        .layer("minecraft:block.vault.deactivate", 0.8f, 1.2f).build()).build(),
                        world, pos.center());
            }
            return true;
        }
        boolean fresh = attackers.putIfAbsent(player.uuid(), tick) == null;
        if (fresh && tick - lastAttackBroadcast >= ATTACK_BROADCAST_TICKS) {
            lastAttackBroadcast = tick;
            broadcast("§c⚠ Das Repair-Totem bei §f" + where(totem) + "§c wird angegriffen!");
        }
        effects.play(hitEffect(), world, pos.center());
        return true;
    }

    /** Der Adapter meldet, dass das Totem abgebaut wurde. */
    public void onDestroyed(PlayerHandle breaker, WorldRef world, BlockPos pos) {
        Active totem = active;
        if (totem == null || !totem.world.equals(world.id()) || !totem.pos().equals(pos)) {
            return;
        }
        String name = breaker == null ? null : breaker.name();
        fail(name, "§c✖ " + (name != null ? name : "Jemand") + "§7 hat das §dRepair-Totem§7 bei §f" + where(totem)
                + "§7 zerstört. Niemand bekommt Energie.");
    }

    /** Abbauzeit in Ticks für den Adapter (fest, unabhängig vom Werkzeug). */
    public int breakTicks() {
        return config.get().totem.breakSeconds * TICKS_PER_SECOND;
    }

    private void tickAttack(Active totem, XmasConfig cfg) {
        if (attackers.isEmpty()) {
            return;
        }
        attackers.keySet().removeIf(uuid -> platform.player(uuid).map(p -> !p.isAlive()).orElse(true));
        long earliest = attackers.values().stream().mapToLong(Long::longValue).min().orElse(tick);
        long elapsed = tick - earliest;
        long total = breakTicks();
        long leftTicks = Math.max(0, total - elapsed);
        boolean finale = leftTicks <= 3L * TICKS_PER_SECOND;
        int interval = finale ? TICKS_PER_SECOND / 2 : TICKS_PER_SECOND;
        if (elapsed % interval == 0) {
            float pitch = 0.6f + 1.3f * (float) elapsed / total;
            effects.play(EffectTimeline.builder().sound(0, SoundStack.builder()
                    .layer("minecraft:block.note_block.bell", 1.2f, Math.min(2f, pitch))
                    .layer("minecraft:block.vault.ambient", 0.8f, 0.8f + pitch * 0.3f)
                    .build()).build(), totem.worldRef(), totem.pos().center());
        }
        // Rot-violette Spirale, die sich mit dem Countdown um das Totem zusammenzieht.
        double p = (double) elapsed / total;
        ParticleSpec spiral = ParticleSpec.transition(DANGER, DANGER_ACCENT, 0.9f);
        effects.play(EffectTimeline.builder().at(0, c -> c.particles(spiral,
                Shapes.spiral(2.2 - 1.6 * p, 2.4, 1.5, 12, tick * 0.3))).build(),
                totem.worldRef(), totem.pos().topCenter().add(0, -1, 0));
    }

    // ------------------------------------------------------------------ Spieler

    @Override
    public void onDeath(PlayerHandle victim, PlayerHandle killer) {
        attackers.remove(victim.uuid());
        if (active != null) {
            eliminate(active, victim.uuid(), victim, "§cDu bist gestorben – das Totem wirkt für dich nicht mehr.");
        }
    }

    @Override
    public void onQuit(UUID uuid) {
        attackers.remove(uuid);
        if (active != null) {
            eliminate(active, uuid, null, null);
        }
    }

    private String hudSuffix(UUID uuid) {
        Active totem = active;
        if (totem == null) {
            return "";
        }
        if (!attackers.isEmpty()) {
            Optional<PlayerHandle> player = platform.player(uuid);
            if (player.isPresent() && isInside(totem, player.get(), config.get())) {
                long earliest = attackers.values().stream().mapToLong(Long::longValue).min().orElse(tick);
                long left = Math.max(0, breakTicks() - (tick - earliest));
                return "§c⚠ Totem " + ((left + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
            }
        }
        if (totem.participants.contains(uuid)) {
            return "§d✚ " + HudService.formatSeconds((totem.totalTicks - totem.ageTicks) * 50);
        }
        return "";
    }

    // ------------------------------------------------------------------ Persistenz

    /** Lädt ein laufendes Totem nach einem Neustart. */
    public void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Active loaded = JsonFiles.readLenient(reader, Active.class);
            if (loaded == null || loaded.world == null) {
                return;
            }
            if (loaded.participants == null) loaded.participants = new LinkedHashSet<>();
            if (loaded.eliminated == null) loaded.eliminated = new LinkedHashSet<>();
            active = loaded;
            reconnectUntilAge = loaded.ageTicks + RECONNECT_GRACE_TICKS;
            startAmbience();
            logger.info("Repair-Totem bei " + where(loaded) + " läuft weiter (" + loaded.participants.size()
                    + " Teilnehmer)");
        } catch (IOException | RuntimeException e) {
            logger.error("totem.json konnte nicht gelesen werden – kein Totem aktiv", e);
        }
    }

    public void save() {
        if (active == null) {
            return;
        }
        try {
            JsonFiles.writeAtomic(file, JsonFiles.GSON.toJson(active));
        } catch (IOException e) {
            logger.error("totem.json konnte nicht gespeichert werden", e);
        }
    }

    /** Admin: Totem ohne Wirkung beenden. */
    public boolean cancel(String actor) {
        if (active == null) {
            return false;
        }
        fail(null, "§7Das Repair-Totem bei §f" + where(active) + "§7 wurde von " + actor + " beendet.");
        return true;
    }

    // ------------------------------------------------------------------ Hilfen

    private void broadcast(String message) {
        for (PlayerHandle player : platform.onlinePlayers()) {
            player.sendMessage(Messages.prefixed(message));
        }
        logger.info(message.replaceAll("§.", ""));
    }

    private static int remaining(Active totem) {
        return totem.participants.size();
    }

    static String where(Active totem) {
        String world = totem.world.startsWith("minecraft:") ? totem.world.substring("minecraft:".length()) : totem.world;
        return totem.x + " " + totem.y + " " + totem.z + " (" + world + ")";
    }

    private void startAmbience() {
        if (ambience != null) {
            ambience.cancel();
        }
        Active totem = active;
        int ticks = (int) Math.max(1, totem.totalTicks - totem.ageTicks);
        ambience = effects.play(ambienceEffect(config.get().totem.radius, ticks), totem.worldRef(),
                totem.pos().topCenter().add(0, -1, 0));
    }

    // ------------------------------------------------------------------ Effekte

    /** Lichtsäule und Ring, der sich vom Totem bis zum Rand des Kreises ausbreitet. */
    static EffectTimeline placeEffect() {
        ParticleSpec beam = ParticleSpec.of("minecraft:end_rod").count(2).spread(0.1, 0.4, 0.1).speed(0.01);
        ParticleSpec wave = ParticleSpec.transition(RING, RING_ACCENT, 1.4f);
        return EffectTimeline.builder()
                .sound(0, SoundStack.builder()
                        .layer("minecraft:block.beacon.activate", 1.2f, 0.9f)
                        .layer("minecraft:block.vault.activate", 1f, 0.8f, 0.05f, 4)
                        .layer("minecraft:item.totem.use", 0.5f, 1.5f, 0.05f, 10)
                        .build())
                .phase(20, Easing.EASE_OUT, (c, p, f) -> {
                    for (int i = 0; i < 6; i++) {
                        c.particle(beam, new Vec3(0, 1.0 + i * 1.5 * p, 0));
                    }
                })
                .phase(30, Easing.EASE_OUT, (c, p, f) -> c.particles(wave,
                        Shapes.ring(1 + 14 * p, 48 + (int) (40 * p), f * 0.05).stream()
                                .map(v -> v.add(0, 0.2, 0)).toList()))
                .build();
    }

    /**
     * Läuft während des ganzen Rituals: schwebendes Repair Kit (Item-Display), langsam rotierende
     * Innenspirale und der Partikelring am Rand des Kreises.
     */
    static EffectTimeline ambienceEffect(int radius, int ticks) {
        ParticleSpec edge = ParticleSpec.transition(RING, RING_ACCENT, 1.2f);
        ParticleSpec inner = ParticleSpec.dust(RING_ACCENT, 0.8f);
        ParticleSpec glow = ParticleSpec.of("minecraft:glow").count(1).spread(0.3);
        int edgePoints = Math.max(32, (int) (radius * 2 * Math.PI / 1.2));
        DisplayHandle[] kit = new DisplayHandle[1];
        return EffectTimeline.builder()
                .during(0, ticks, (c, p, f) -> {
                    if (kit[0] == null) {
                        kit[0] = c.display(DisplaySpec.item(CustomItem.REPAIR_KIT.itemId()).scale(0.9).fullBright()
                                .glow(RING), new Vec3(0, 2.0, 0));
                    }
                    if (f % 4 == 0) {
                        double bob = Math.sin(f * 0.08) * 0.15;
                        kit[0].moveTo(c.origin().add(0, 2.0 + bob, 0), 4);
                        kit[0].transform(DisplayTransform.scaled(0.9).withRotation(f * 3f, 0, 0), 4);
                    }
                    if (f % 5 == 0) {
                        c.particles(edge, Shapes.ring(radius, edgePoints, f * 0.01).stream()
                                .map(v -> v.add(0, 0.2, 0)).toList());
                    }
                    if (f % 2 == 0) {
                        c.particles(inner, Shapes.spiral(1.2, 2.2, 1, 6, f * 0.12));
                    }
                    if (f % 10 == 0) {
                        c.particle(glow, new Vec3(0, 2.0, 0));
                    }
                    if (f % 80 == 0) {
                        c.sound(SoundStack.builder().layer("minecraft:block.beacon.ambient", 0.8f, 1.2f).build());
                    }
                })
                .build();
    }

    static EffectTimeline hitEffect() {
        ParticleSpec splinters = ParticleSpec.transition(DANGER, DANGER_ACCENT, 1.0f).count(6).spread(0.35).speed(0.1);
        ParticleSpec crit = ParticleSpec.of("minecraft:crit").count(4).spread(0.3).speed(0.3);
        return EffectTimeline.builder()
                .at(0, c -> {
                    c.particle(splinters, Vec3.ZERO);
                    c.particle(crit, Vec3.ZERO);
                })
                .build();
    }

    /** Explosion aus Totem- und End-Rod-Partikeln, der Ring fällt in sich zusammen. */
    static EffectTimeline shatterEffect(Rgb color, Rgb accent, int radius) {
        ParticleSpec totem = ParticleSpec.of("minecraft:totem_of_undying").count(60).spread(0.5).speed(0.8);
        ParticleSpec rods = ParticleSpec.of("minecraft:end_rod").count(30).spread(0.3).speed(0.35);
        ParticleSpec collapse = ParticleSpec.transition(color, accent, 1.3f);
        return EffectTimeline.builder()
                .sound(0, SoundStack.builder()
                        .layer("minecraft:block.glass.break", 1.4f, 0.7f, 0.05f, 0)
                        .layer("minecraft:block.beacon.deactivate", 1.2f, 0.8f, 0f, 2)
                        .layer("minecraft:block.amethyst_cluster.break", 1f, 0.6f, 0.1f, 4)
                        .build())
                .at(0, c -> {
                    c.particle(totem, Vec3.ZERO);
                    c.particle(rods, Vec3.ZERO);
                })
                .phase(24, Easing.EASE_IN, (c, p, f) -> c.particles(collapse,
                        Shapes.ring(radius * (1 - p) + 0.3, 40, f * 0.1).stream().map(v -> v.add(0, -0.3, 0)).toList()))
                .build();
    }
}
