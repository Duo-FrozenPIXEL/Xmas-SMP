package de.xmassmp.admin;

import de.xmassmp.config.ConfigManager;
import de.xmassmp.crystal.Crystal;
import de.xmassmp.crystal.CrystalModels;
import de.xmassmp.crystal.CrystalRegistry;
import de.xmassmp.crystal.PristineStage;
import de.xmassmp.event.EventDefinition;
import de.xmassmp.event.EventService;
import de.xmassmp.event.RunningEvent;
import de.xmassmp.item.CustomItem;
import de.xmassmp.item.ItemCatalog;
import de.xmassmp.item.ItemSpec;
import de.xmassmp.player.PlayerData;
import de.xmassmp.player.PlayerDataStore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Plattformneutrale Logik hinter {@code /xmas}. Die Plattform kümmert sich um Befehlsbaum,
 * Rechte und Spielerauflösung und gibt die Antworttexte (§-Farbcodes) aus.
 */
public final class AdminService {

    /** Permission für alle Admin-Befehle; ohne Permission-Plugin gilt OP-Level 2. */
    public static final String PERMISSION = "xmassmp.admin";
    public static final int FALLBACK_OP_LEVEL = 2;
    public static final String CONSOLE = "Konsole";

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public record Result(boolean success, String message) {
        static Result ok(String message) {
            return new Result(true, message);
        }

        static Result fail(String message) {
            return new Result(false, message);
        }
    }

    private final CrystalRegistry crystals;
    private final PlayerDataStore players;
    private final ConfigManager config;
    private final EventService events;
    private final AuditLog audit;
    private final Supplier<List<String>> reloader;

    public AdminService(CrystalRegistry crystals, PlayerDataStore players, ConfigManager config,
                        EventService events, AuditLog audit, Supplier<List<String>> reloader) {
        this.crystals = crystals;
        this.players = players;
        this.config = config;
        this.events = events;
        this.audit = audit;
        this.reloader = reloader;
    }

    public static List<String> help() {
        return List.of(
                "§6/xmas give <spieler> crystal <kristall> [tier] [zustand]",
                "§6/xmas give <spieler> item <item>",
                "§6/xmas set <spieler> crystal|tier|energy <wert>",
                "§6/xmas reload",
                "§6/xmas event list|start <id>|stop <id>",
                "§6/xmas event simulate <yyyy-MM-dd> <HH:mm> [dry] §7· §6/xmas event simulate off");
    }

    // ------------------------------------------------------------------ give

    public Result giveCrystal(String actor, String target, String crystalId, int tier, String stageId,
                              Consumer<ItemSpec> giver) {
        Optional<Crystal> crystal = crystals.get(crystalId);
        if (crystal.isEmpty()) {
            return Result.fail("Unbekannter Kristall: " + crystalId);
        }
        if (!CrystalModels.isValidTier(tier)) {
            return Result.fail("Tier muss " + CrystalModels.MIN_TIER + "–" + CrystalModels.MAX_TIER + " sein");
        }
        Optional<PristineStage> stage = PristineStage.byId(stageId);
        if (stage.isEmpty()) {
            return Result.fail("Unbekannter Zustand: " + stageId + " (" + String.join(", ", stageIds()) + ")");
        }
        giver.accept(ItemCatalog.crystal(crystal.get(), tier, stage.get()));
        audit.record(actor, "give", target, "crystal=" + crystalId + " tier=" + tier + " stage=" + stageId);
        return Result.ok(target + " hat einen " + crystal.get().nameFallback() + "-Kristall (Tier " + tier
                + ", " + stageId + ") erhalten");
    }

    public Result giveItem(String actor, String target, String itemId, Consumer<ItemSpec> giver) {
        Optional<CustomItem> item = CustomItem.byId(itemId);
        if (item.isEmpty()) {
            return Result.fail("Unbekanntes Item: " + itemId);
        }
        giver.accept(ItemCatalog.custom(item.get()));
        audit.record(actor, "give", target, "item=" + itemId);
        return Result.ok(target + " hat " + item.get().displayName().fallback() + " erhalten");
    }

    // ------------------------------------------------------------------ set

    public Result setCrystal(String actor, UUID uuid, String name, String crystalId) {
        Optional<Crystal> crystal = crystals.get(crystalId);
        if (crystal.isEmpty()) {
            return Result.fail("Unbekannter Kristall: " + crystalId);
        }
        PlayerData data = players.get(uuid);
        String before = data.crystalId();
        data.setCrystalId(crystalId);
        players.save(uuid);
        audit.record(actor, "set_crystal", name, before + " -> " + crystalId);
        String note = config.get().crystal(crystalId).enabled ? "" : " §e(Kristall ist in der Config deaktiviert)";
        return Result.ok("Kristall von " + name + " ist jetzt " + crystal.get().nameFallback() + note);
    }

    public Result setTier(String actor, UUID uuid, String name, int tier) {
        if (!CrystalModels.isValidTier(tier)) {
            return Result.fail("Tier muss " + CrystalModels.MIN_TIER + "–" + CrystalModels.MAX_TIER + " sein");
        }
        PlayerData data = players.get(uuid);
        int before = data.tier();
        data.setTier(tier);
        players.save(uuid);
        audit.record(actor, "set_tier", name, before + " -> " + tier);
        return Result.ok("Tier von " + name + " ist jetzt " + tier);
    }

    public Result setEnergy(String actor, UUID uuid, String name, int energy) {
        int min = config.get().energy.min;
        int max = config.get().energy.max;
        if (energy < min || energy > max) {
            return Result.fail("Energie muss " + min + "–" + max + " sein");
        }
        PlayerData data = players.get(uuid);
        int before = data.energy();
        data.setEnergy(energy);
        players.save(uuid);
        audit.record(actor, "set_energy", name, before + " -> " + energy);
        return Result.ok("Energie von " + name + " ist jetzt " + energy);
    }

    // ------------------------------------------------------------------ reload

    public List<String> reload(String actor) {
        List<String> messages = reloader.get();
        audit.record(actor, "reload", null, String.join("; ", messages));
        return messages;
    }

    // ------------------------------------------------------------------ events

    public List<String> eventList() {
        List<String> lines = new ArrayList<>();
        var scheduler = events.scheduler();
        lines.add("§6Events §7(Zeitzone " + scheduler.zone() + ")"
                + (events.isSimulating() ? " §d[Testmodus: " + format(events.clock().now()) + "]" : ""));
        if (scheduler.definitions().isEmpty()) {
            lines.add("§7Keine Events in events.jsonc");
        }
        for (EventDefinition def : scheduler.definitions()) {
            Optional<RunningEvent> running = scheduler.running(def.id());
            String status;
            if (running.isPresent()) {
                status = "§aläuft" + (running.get().end() == null ? " bis zum Stopp"
                        : " bis " + format(running.get().end()));
            } else if (!def.enabled()) {
                status = "§8deaktiviert";
            } else {
                status = events.nextStart(def.id()).map(t -> "§7nächster Start " + format(t))
                        .orElse("§7kein weiterer Termin");
            }
            lines.add("§e" + def.id() + " §7(" + def.name() + ") – " + status);
        }
        return lines;
    }

    public Result eventStart(String actor, String id) {
        Optional<EventDefinition> def = events.scheduler().definition(id);
        if (def.isEmpty()) {
            return Result.fail("Unbekanntes Event: " + id);
        }
        if (!events.start(id)) {
            return Result.fail("Event " + id + " läuft bereits");
        }
        audit.record(actor, "event_start", id, null);
        return Result.ok(def.get().duration().isZero()
                ? "Event " + id + " ausgelöst"
                : "Event " + id + " gestartet (" + def.get().duration().toMinutes() + " min)");
    }

    public Result eventStop(String actor, String id) {
        if (!events.stop(id)) {
            return Result.fail("Event " + id + " läuft nicht");
        }
        audit.record(actor, "event_stop", id, null);
        return Result.ok("Event " + id + " beendet");
    }

    public Result eventSimulate(String actor, String date, String time, boolean dryRun) {
        Instant target;
        try {
            target = ZonedDateTime.of(LocalDate.parse(date), LocalTime.parse(time), events.scheduler().zone())
                    .toInstant();
        } catch (DateTimeParseException e) {
            return Result.fail("Format: /xmas event simulate yyyy-MM-dd HH:mm [dry]");
        }
        events.simulate(target, dryRun);
        audit.record(actor, "event_simulate", null, date + " " + time + (dryRun ? " dry" : ""));
        return Result.ok("Testmodus aktiv: simulierte Zeit " + format(target)
                + (dryRun ? " – Aktionen werden nur geloggt" : " – Aktionen werden ausgeführt!")
                + " Beenden mit /xmas event simulate off");
    }

    public Result eventSimulateOff(String actor) {
        if (!events.stopSimulation()) {
            return Result.fail("Testmodus ist nicht aktiv");
        }
        audit.record(actor, "event_simulate_off", null, null);
        return Result.ok("Testmodus beendet, echter Event-Zustand wiederhergestellt");
    }

    // ------------------------------------------------------------------ Tab-Completion

    public List<String> crystalIds() {
        return crystals.ids();
    }

    public static List<String> itemIds() {
        return Arrays.stream(CustomItem.values()).map(CustomItem::id).toList();
    }

    public static List<String> stageIds() {
        return Arrays.stream(PristineStage.values()).map(PristineStage::id).toList();
    }

    public List<String> eventIds() {
        return events.scheduler().definitions().stream().map(EventDefinition::id).toList();
    }

    public List<String> runningEventIds() {
        return events.scheduler().running().stream().map(RunningEvent::id).toList();
    }

    /** Vorschläge für das Simulationsdatum: heute und die Weihnachtstage dieses Jahres. */
    public List<String> simulateDateSuggestions() {
        LocalDate today = events.clock().realNow().atZone(events.scheduler().zone()).toLocalDate();
        int year = today.getYear();
        return List.of(today.toString(), year + "-12-24", year + "-12-25", year + "-12-31", (year + 1) + "-01-01");
    }

    private String format(Instant instant) {
        return DISPLAY.format(instant.atZone(events.scheduler().zone()));
    }
}
