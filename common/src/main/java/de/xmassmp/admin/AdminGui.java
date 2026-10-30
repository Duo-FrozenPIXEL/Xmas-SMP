package de.xmassmp.admin;

import de.xmassmp.config.ConfigManager;
import de.xmassmp.config.RecipeSpec;
import de.xmassmp.config.XmasConfig;
import de.xmassmp.crystal.Crystal;
import de.xmassmp.crystal.CrystalModels;
import de.xmassmp.crystal.CrystalRegistry;
import de.xmassmp.event.EventDefinition;
import de.xmassmp.event.EventService;
import de.xmassmp.event.RunningEvent;
import de.xmassmp.gui.GuiButton;
import de.xmassmp.gui.GuiClick;
import de.xmassmp.gui.GuiHost;
import de.xmassmp.gui.GuiView;
import de.xmassmp.item.CustomItem;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.player.PlayerData;
import de.xmassmp.player.PlayerDataStore;
import de.xmassmp.text.Messages;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Konfigurations-GUI für {@code /xmas admin}. Jede Seite wird nach jedem Klick neu aufgebaut und
 * zeigt so immer den aktuellen Stand. Alle Änderungen laufen über {@link AdminService}: Sie werden
 * im Audit-Log festgehalten, sofort gespeichert (Kommentare in den Dateien bleiben erhalten) und
 * wirken ohne Reload.
 */
public final class AdminGui {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
    private static final int LIST_SLOTS = 45;
    private static final String INT_HINT = "§8Links +1 · Rechts −1 · mit Shift ×5";
    private static final String DOUBLE_HINT = "§8Links +0.1 · Rechts −0.1 · mit Shift ×5";

    private final AdminService admin;
    private final ConfigManager config;
    private final CrystalRegistry crystals;
    private final EventService events;
    private final PlayerDataStore players;
    private final Supplier<List<PlayerHandle>> online;
    private final GuiHost host;
    private Supplier<Optional<String>> totemStatus = Optional::empty;
    private Predicate<String> totemCancel = actor -> false;

    public AdminGui(AdminService admin, ConfigManager config, CrystalRegistry crystals, EventService events,
                    PlayerDataStore players, Supplier<List<PlayerHandle>> online, GuiHost host) {
        this.admin = admin;
        this.config = config;
        this.crystals = crystals;
        this.events = events;
        this.players = players;
        this.online = online;
        this.host = host;
    }

    /** Öffnet das Hauptmenü. {@code false}, wenn die Plattform keine GUIs kann. */
    public boolean open(PlayerHandle viewer) {
        return show(viewer, main(viewer));
    }

    private boolean show(PlayerHandle viewer, GuiView view) {
        return host.open(viewer.uuid(), view);
    }

    // ------------------------------------------------------------------ Seiten

    GuiView main(PlayerHandle viewer) {
        GuiView.Builder b = GuiView.builder("§6Xmas-SMP §8– Admin", 3);
        b.set(1, 1, nav(viewer, "minecraft:comparator", "§eAllgemein", this::general,
                "§7Plugin, Fähigkeiten, PvP-Energie, Reload"));
        b.set(1, 2, nav(viewer, "minecraft:amethyst_shard", "§dKristalle", this::crystalsPage,
                "§7Aktivieren, Cooldown- und Schadens-Multiplikator"));
        b.set(1, 3, nav(viewer, "minecraft:glowstone_dust", "§6Energie", this::energy,
                "§7Start, Grenzen, Gewinn und Verlust"));
        b.set(1, 4, nav(viewer, "minecraft:player_head", "§bSpieler", this::playersPage,
                "§7Kristall, Tier, Energie, Cooldowns"));
        b.set(1, 5, nav(viewer, "minecraft:clock", "§aEvents", this::eventsPage,
                "§7Ansehen, (de)aktivieren, starten und stoppen"));
        b.set(1, 6, nav(viewer, "minecraft:chest", "§fItems", this::items, "§7Custom-Items herausnehmen"));
        b.set(1, 7, nav(viewer, "minecraft:emerald", "§aGameplay", this::gameplay,
                "§7Energy Bottle, Upgrader, Händler, Totem, Rezepte"));
        b.set(2, 8, GuiButton.action("minecraft:barrier", "§cSchließen", c -> host.close(viewer.uuid())));
        return b.build();
    }

    GuiView general(PlayerHandle viewer) {
        XmasConfig cfg = config.get();
        Function<PlayerHandle, GuiView> page = this::general;
        GuiView.Builder b = GuiView.builder("§8Admin › §eAllgemein", 3);
        b.set(1, 1, toggle(viewer, "Plugin", cfg.general.enabled, List.of("general", "enabled"), page,
                "§7Hauptschalter für alles"));
        b.set(1, 2, toggle(viewer, "Fähigkeiten", cfg.general.abilitiesEnabled,
                List.of("general", "abilitiesEnabled"), page, "§7Alle Kristall-Kräfte global"));
        b.set(1, 3, toggle(viewer, "PvP-Energie", cfg.energy.pvpEnabled, List.of("energy", "pvpEnabled"), page,
                "§7Energie-Gewinn und -Verlust", "§7durch Kills und Tode"));
        b.set(1, 4, toggle(viewer, "Kristall beim ersten Join", cfg.crystalItem.giveOnJoin,
                List.of("crystalItem", "giveOnJoin"), page));
        b.set(1, 5, toggle(viewer, "Seelengebunden", cfg.crystalItem.soulbound,
                List.of("crystalItem", "soulbound"), page, "§7Kristall kann nicht verloren gehen"));
        b.set(1, 6, toggle(viewer, "Aktivierung mit F", cfg.controls.swapHandsActivates,
                List.of("controls", "swapHandsActivates"), page));
        b.set(1, 7, toggle(viewer, "HUD", cfg.hud.enabled, List.of("hud", "enabled"), page,
                "§7Anzeige über der Hotbar"));
        b.set(2, 4, GuiButton.action("minecraft:repeater", "§eNeu laden", c -> {
            ConfigManager.LoadResult result = admin.reload(viewer.name());
            viewer.sendMessage(result.success() ? Messages.prefixed("Neu geladen")
                    : Messages.PREFIX + "§cNeu laden fehlgeschlagen: " + String.join("; ", result.messages()));
            show(viewer, general(viewer));
        }, "§7config.jsonc und events.jsonc"));
        b.set(2, 0, back(viewer, this::main));
        return b.build();
    }

    GuiView crystalsPage(PlayerHandle viewer) {
        XmasConfig cfg = config.get();
        Function<PlayerHandle, GuiView> page = this::crystalsPage;
        GuiView.Builder b = GuiView.builder("§8Admin › §dKristalle", 5);
        List<Crystal> all = new ArrayList<>(crystals.all());
        int first = Math.max(0, (GuiView.COLUMNS - all.size()) / 2);
        for (int i = 0; i < all.size() && first + i < GuiView.COLUMNS; i++) {
            Crystal crystal = all.get(i);
            XmasConfig.CrystalSettings settings = cfg.crystal(crystal.id());
            int column = first + i;
            String name = "§" + crystal.chatColor() + crystal.nameFallback();
            b.set(0, column, GuiButton.label("minecraft:echo_shard", name, "§8ID: " + crystal.id()));
            b.set(1, column, toggle(viewer, crystal.nameFallback(), settings.enabled,
                    List.of("crystals", crystal.id(), "enabled"), page));
            b.set(2, column, doubleValue(viewer, "minecraft:clock", "Cooldown ×", settings.cooldownMultiplier,
                    List.of("crystals", crystal.id(), "cooldownMultiplier"), page));
            b.set(3, column, doubleValue(viewer, "minecraft:iron_sword", "Schaden ×", settings.damageMultiplier,
                    List.of("crystals", crystal.id(), "damageMultiplier"), page));
        }
        b.set(4, 0, back(viewer, this::main));
        return b.build();
    }

    GuiView energy(PlayerHandle viewer) {
        XmasConfig.Energy cfg = config.get().energy;
        Function<PlayerHandle, GuiView> page = this::energy;
        GuiView.Builder b = GuiView.builder("§8Admin › §6Energie", 3);
        b.set(1, 1, intValue(viewer, "minecraft:glowstone_dust", "Start", cfg.start,
                List.of("energy", "start"), page, "§7Energie neuer Spieler"));
        b.set(1, 2, intValue(viewer, "minecraft:redstone", "Minimum", cfg.min, List.of("energy", "min"), page));
        b.set(1, 3, intValue(viewer, "minecraft:glowstone", "Maximum", cfg.max, List.of("energy", "max"), page));
        b.set(1, 5, intValue(viewer, "minecraft:diamond_sword", "Gewinn pro Kill", cfg.killGain,
                List.of("energy", "killGain"), page));
        b.set(1, 6, intValue(viewer, "minecraft:skeleton_skull", "Verlust pro Tod", cfg.deathLoss,
                List.of("energy", "deathLoss"), page));
        b.set(1, 7, intValue(viewer, "minecraft:clock", "Anti-Farming (min)", cfg.sameVictimCooldownMinutes,
                List.of("energy", "sameVictimCooldownMinutes"), page,
                "§7Kein Gewinn für denselben Gegner", "§7innerhalb dieser Minuten; 0 = aus"));
        b.set(2, 0, back(viewer, this::main));
        return b.build();
    }

    GuiView playersPage(PlayerHandle viewer) {
        GuiView.Builder b = GuiView.builder("§8Admin › §bSpieler", 6);
        List<PlayerHandle> list = new ArrayList<>(online.get());
        list.sort(Comparator.comparing(PlayerHandle::name, String.CASE_INSENSITIVE_ORDER));
        if (list.isEmpty()) {
            b.set(2, 4, GuiButton.label("minecraft:barrier", "§7Keine Spieler online"));
        }
        for (int i = 0; i < list.size() && i < LIST_SLOTS; i++) {
            PlayerHandle target = list.get(i);
            PlayerData data = players.get(target.uuid());
            b.set(i, GuiButton.action("minecraft:player_head", "§b" + target.name(),
                    c -> show(viewer, playerPage(viewer, target.uuid(), target.name())),
                    "§7Kristall: §f" + crystalName(data.crystalId()),
                    "§7Tier " + data.tier() + " · Energie " + data.energy()));
        }
        if (list.size() > LIST_SLOTS) {
            b.set(5, 4, GuiButton.label("minecraft:paper", "§7… und " + (list.size() - LIST_SLOTS) + " weitere",
                    "§8Per Befehl: /xmas set <spieler> …"));
        }
        b.set(5, 0, back(viewer, this::main));
        return b.build();
    }

    GuiView playerPage(PlayerHandle viewer, UUID uuid, String name) {
        PlayerData data = players.get(uuid);
        Function<PlayerHandle, GuiView> page = v -> playerPage(v, uuid, name);
        String actor = viewer.name();
        GuiView.Builder b = GuiView.builder("§8Admin › Spieler › §b" + name, 3);
        List<String> ids = crystals.ids();
        b.set(1, 1, GuiButton.action("minecraft:echo_shard", "§eKristall: §f" + crystalName(data.crystalId()), c -> {
            if (ids.isEmpty()) {
                return;
            }
            int current = Math.max(0, ids.indexOf(data.crystalId()));
            int next = Math.floorMod(current + (c.right() ? -1 : 1), ids.size());
            change(viewer, admin.setCrystal(actor, uuid, name, ids.get(next)), page);
        }, "§8Links nächster · Rechts vorheriger"));
        b.set(1, 3, GuiButton.action("minecraft:experience_bottle", "§eTier: §f" + data.tier(), c -> {
            int next = data.tier() >= CrystalModels.MAX_TIER ? CrystalModels.MIN_TIER : data.tier() + 1;
            change(viewer, admin.setTier(actor, uuid, name, next), page);
        }, "§8Klick: wechseln"));
        XmasConfig.Energy energy = config.get().energy;
        b.set(1, 5, GuiButton.action("minecraft:glowstone_dust", "§eEnergie: §f" + data.energy(), c -> {
            int step = c.shift() ? 5 : 1;
            int next = Math.max(energy.min, Math.min(energy.max, data.energy() + (c.right() ? -step : step)));
            change(viewer, admin.setEnergy(actor, uuid, name, next), page);
        }, "§7Bereich " + energy.min + "–" + energy.max, INT_HINT));
        b.set(1, 7, GuiButton.action("minecraft:milk_bucket", "§eCooldowns zurücksetzen",
                c -> change(viewer, admin.resetCooldowns(actor, uuid, name), page)));
        b.set(2, 0, back(viewer, this::playersPage));
        return b.build();
    }

    GuiView eventsPage(PlayerHandle viewer) {
        GuiView.Builder b = GuiView.builder("§8Admin › §aEvents", 6);
        Function<PlayerHandle, GuiView> page = this::eventsPage;
        String actor = viewer.name();
        List<EventDefinition> defs = new ArrayList<>(events.scheduler().definitions());
        if (defs.isEmpty()) {
            b.set(2, 4, GuiButton.label("minecraft:barrier", "§7Keine Events in events.jsonc"));
        }
        for (int i = 0; i < defs.size() && i < LIST_SLOTS; i++) {
            EventDefinition def = defs.get(i);
            Optional<RunningEvent> running = events.scheduler().running(def.id());
            String icon = running.isPresent() ? "minecraft:lime_concrete"
                    : def.enabled() ? "minecraft:clock" : "minecraft:gray_concrete";
            b.set(i, new GuiButton(icon, "§e" + def.name() + " §8(" + def.id() + ")", List.of(
                    eventStatus(def, running),
                    running.isPresent() ? "§7Links: stoppen" : "§7Links: jetzt starten",
                    def.enabled() ? "§7Rechts: automatischen Start ausschalten"
                            : "§7Rechts: automatischen Start einschalten"),
                    running.isPresent(), c -> {
                        if (c.right()) {
                            change(viewer, admin.eventSetEnabled(actor, def.id(), !def.enabled()), page);
                        } else if (running.isPresent()) {
                            change(viewer, admin.eventStop(actor, def.id()), page);
                        } else {
                            change(viewer, admin.eventStart(actor, def.id()), page);
                        }
                    }));
        }
        if (events.isSimulating()) {
            b.set(5, 4, GuiButton.action("minecraft:ender_eye",
                    "§dTestmodus: " + format(events.clock().now()) + (events.isDryRun() ? " §7(Trockenlauf)" : ""),
                    c -> change(viewer, admin.eventSimulateOff(actor), page), "§7Klick: Testmodus beenden"));
        } else {
            b.set(5, 4, GuiButton.label("minecraft:ender_pearl", "§7Testmodus aus",
                    "§8Starten mit /xmas event simulate", "§8<yyyy-MM-dd> <HH:mm> [dry]"));
        }
        b.set(5, 0, back(viewer, this::main));
        return b.build();
    }

    /** Optional: Status und Abbruch des laufenden Repair-Totems (setzt der Kern). */
    public void setTotemControls(Supplier<Optional<String>> status, Predicate<String> cancel) {
        this.totemStatus = status;
        this.totemCancel = cancel;
    }

    GuiView gameplay(PlayerHandle viewer) {
        XmasConfig cfg = config.get();
        GuiView.Builder b = GuiView.builder("§8Admin › §fGameplay", 5);
        Function<PlayerHandle, GuiView> page = this::gameplay;
        b.set(0, 1, toggle(viewer, "Energy Bottles", cfg.energyBottle.enabled, List.of("energyBottle", "enabled"), page,
                "§7Abfüllen, Trinken und Zerlegen"));
        b.set(0, 2, intValue(viewer, "minecraft:glass_bottle", "Ladezeit (s)", cfg.energyBottle.chargeSeconds,
                List.of("energyBottle", "chargeSeconds"), page));
        b.set(0, 3, intValue(viewer, "minecraft:iron_sword", "Kampfsperre (s)", cfg.energyBottle.combatTagSeconds,
                List.of("energyBottle", "combatTagSeconds"), page));
        b.set(0, 5, toggle(viewer, "Upgrader", cfg.upgrader.enabled, List.of("upgrader", "enabled"), page));
        b.set(0, 6, toggle(viewer, "Händler", cfg.trader.enabled, List.of("trader", "enabled"), page));
        b.set(0, 7, intValue(viewer, "minecraft:clock", "Händler-Wartezeit (min)", cfg.trader.cooldownMinutes,
                List.of("trader", "cooldownMinutes"), page));

        b.set(1, 1, toggle(viewer, "Repair-Totem", cfg.totem.enabled, List.of("totem", "enabled"), page));
        b.set(1, 2, intValue(viewer, "minecraft:compass", "Radius", cfg.totem.radius, List.of("totem", "radius"), page));
        b.set(1, 3, intValue(viewer, "minecraft:clock", "Dauer (min)", cfg.totem.durationMinutes,
                List.of("totem", "durationMinutes"), page));
        b.set(1, 4, intValue(viewer, "minecraft:glowstone_dust", "Ziel-Energie", cfg.totem.restoreTo,
                List.of("totem", "restoreTo"), page));
        b.set(1, 5, intValue(viewer, "minecraft:netherite_pickaxe", "Abbauzeit (s)", cfg.totem.breakSeconds,
                List.of("totem", "breakSeconds"), page));
        b.set(1, 6, intValue(viewer, "minecraft:lead", "Teilnahme-Fenster (s)", cfg.totem.joinSeconds,
                List.of("totem", "joinSeconds"), page));
        Optional<String> running = totemStatus.get();
        b.set(1, 7, running.isPresent()
                ? GuiButton.action("minecraft:tnt", "§cTotem beenden", c -> {
                    totemCancel.test(viewer.name());
                    show(viewer, gameplay(viewer));
                }, "§7" + running.get(), "§8Klick: ohne Wirkung beenden")
                : GuiButton.label("minecraft:gray_dye", "§7Kein Totem aktiv"));

        int column = 1;
        for (Map.Entry<String, RecipeSpec> recipe : cfg.recipes.entrySet()) {
            if (column > 7) {
                break;
            }
            b.set(2, column++, toggle(viewer, "Rezept " + recipe.getKey(), recipe.getValue().enabled,
                    List.of("recipes", recipe.getKey(), "enabled"), page, "§8Muster in config.jsonc"));
        }
        b.set(4, 0, back(viewer, this::main));
        return b.build();
    }

    GuiView items(PlayerHandle viewer) {
        GuiView.Builder b = GuiView.builder("§8Admin › §fItems", 3);
        String actor = viewer.name();
        CustomItem[] custom = CustomItem.values();
        for (int i = 0; i < custom.length && i < GuiView.COLUMNS; i++) {
            CustomItem item = custom[i];
            b.set(0, i, GuiButton.action(item.baseItem(), "§f" + item.displayName().fallback(),
                    c -> report(viewer, admin.giveItem(actor, actor, item.id(), viewer::giveItem)),
                    "§7Klick: ins Inventar"));
        }
        List<Crystal> all = new ArrayList<>(crystals.all());
        for (int i = 0; i < all.size() && i < GuiView.COLUMNS; i++) {
            Crystal crystal = all.get(i);
            b.set(1, i, GuiButton.action("minecraft:echo_shard", "§" + crystal.chatColor() + crystal.nameFallback(),
                    c -> report(viewer, admin.giveCrystal(actor, actor, crystal.id(), c.shift() ? 2 : 1,
                            "pristine", viewer::giveItem)),
                    "§7Klick: Tier 1 · Shift-Klick: Tier 2", "§8Ungebunden, wie /xmas give"));
        }
        b.set(2, 0, back(viewer, this::main));
        return b.build();
    }

    // ------------------------------------------------------------------ Bausteine

    private GuiButton nav(PlayerHandle viewer, String icon, String name, Function<PlayerHandle, GuiView> page,
                          String description) {
        return GuiButton.action(icon, name, c -> show(viewer, page.apply(viewer)), description);
    }

    private GuiButton back(PlayerHandle viewer, Function<PlayerHandle, GuiView> page) {
        return GuiButton.action("minecraft:arrow", "§7Zurück", c -> show(viewer, page.apply(viewer)));
    }

    private GuiButton toggle(PlayerHandle viewer, String label, boolean value, List<String> path,
                             Function<PlayerHandle, GuiView> page, String... description) {
        List<String> lore = new ArrayList<>(List.of(description));
        lore.add("§8Klick: " + (value ? "ausschalten" : "einschalten"));
        return new GuiButton(value ? "minecraft:lime_dye" : "minecraft:gray_dye",
                (value ? "§a" : "§c") + label + ": " + (value ? "an" : "aus"), lore, false,
                c -> change(viewer, admin.setConfig(viewer.name(), path, !value), page));
    }

    private GuiButton intValue(PlayerHandle viewer, String icon, String label, int value, List<String> path,
                               Function<PlayerHandle, GuiView> page, String... description) {
        List<String> lore = new ArrayList<>(List.of(description));
        lore.add(INT_HINT);
        return new GuiButton(icon, "§e" + label + ": §f" + value, lore, false, c -> {
            int next = Math.max(0, value + signedStep(c, 1));
            change(viewer, admin.setConfig(viewer.name(), path, next), page);
        });
    }

    private GuiButton doubleValue(PlayerHandle viewer, String icon, String label, double value, List<String> path,
                                  Function<PlayerHandle, GuiView> page) {
        return new GuiButton(icon, "§e" + label + " §f" + value, List.of(DOUBLE_HINT), false, c -> {
            double next = Math.max(0, Math.round((value + signedStep(c, 1) * 0.1) * 100) / 100.0);
            change(viewer, admin.setConfig(viewer.name(), path, next), page);
        });
    }

    private static int signedStep(GuiClick click, int step) {
        int amount = click.shift() ? step * 5 : step;
        return click.right() ? -amount : amount;
    }

    /** Fehler als Chat-Meldung, danach die Seite mit dem neuen Stand. */
    private void change(PlayerHandle viewer, AdminService.Result result, Function<PlayerHandle, GuiView> page) {
        if (!result.success()) {
            viewer.sendMessage(Messages.PREFIX + "§c" + result.message());
        }
        show(viewer, page.apply(viewer));
    }

    private static void report(PlayerHandle viewer, AdminService.Result result) {
        viewer.sendMessage(result.success() ? Messages.prefixed(result.message())
                : Messages.PREFIX + "§c" + result.message());
    }

    private String crystalName(String id) {
        if (id == null) {
            return "keiner";
        }
        return crystals.get(id).map(Crystal::nameFallback).orElse(id + " (unbekannt)");
    }

    private String eventStatus(EventDefinition def, Optional<RunningEvent> running) {
        if (running.isPresent()) {
            return "§aläuft" + (running.get().end() == null ? " bis zum Stopp" : " bis " + format(running.get().end()));
        }
        if (!def.enabled()) {
            return "§8automatischer Start aus";
        }
        return events.nextStart(def.id()).map(t -> "§7nächster Start " + format(t)).orElse("§7kein weiterer Termin");
    }

    private String format(Instant instant) {
        return DISPLAY.format(instant.atZone(events.scheduler().zone()));
    }
}
