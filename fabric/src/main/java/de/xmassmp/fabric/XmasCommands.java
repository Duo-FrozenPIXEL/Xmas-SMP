package de.xmassmp.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import de.xmassmp.XmasCore;
import de.xmassmp.admin.AdminService;
import de.xmassmp.bottle.BottleRegistry;
import de.xmassmp.config.ConfigManager;
import de.xmassmp.energy.EnergyLedger;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.text.Messages;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * {@code /xmas} – Admin-Befehle mit Tab-Completion. Recht {@code xmassmp.admin}, ohne
 * Permission-Plugin OP-Level 2. Die Logik liegt in {@link AdminService}.
 */
final class XmasCommands {

    private static final SimpleCommandExceptionType NOT_STARTED = new SimpleCommandExceptionType(
            FabricText.legacy(Messages.PREFIX + "§cXmas-SMP ist nicht gestartet – Details im Server-Log"));

    private final Supplier<XmasCore> core;

    private XmasCommands(Supplier<XmasCore> core) {
        this.core = core;
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher, Supplier<XmasCore> core) {
        new XmasCommands(core).register(dispatcher);
    }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("xmas")
                .requires(Permissions.require(AdminService.PERMISSION, AdminService.FALLBACK_OP_LEVEL))
                .executes(this::help)
                .then(literal("give").then(argument("player", EntityArgument.player())
                        .then(literal("crystal").then(argument("crystal", StringArgumentType.word())
                                .suggests(suggest(AdminService::crystalIds))
                                .executes(ctx -> giveCrystal(ctx, 1, "pristine"))
                                .then(argument("tier", IntegerArgumentType.integer(1, 2))
                                        .executes(ctx -> giveCrystal(ctx, IntegerArgumentType.getInteger(ctx, "tier"), "pristine"))
                                        .then(argument("stage", StringArgumentType.word())
                                                .suggests(suggest(admin -> AdminService.stageIds()))
                                                .executes(ctx -> giveCrystal(ctx, IntegerArgumentType.getInteger(ctx, "tier"),
                                                        StringArgumentType.getString(ctx, "stage")))))))
                        .then(literal("item").then(argument("item", StringArgumentType.word())
                                .suggests(suggest(admin -> AdminService.itemIds()))
                                .executes(this::giveItem)))))
                .then(literal("set").then(argument("player", EntityArgument.player())
                        .then(literal("crystal").then(argument("crystal", StringArgumentType.word())
                                .suggests(suggest(AdminService::crystalIds))
                                .executes(ctx -> withPlayer(ctx, (admin, p) -> admin.setCrystal(actor(ctx),
                                        p.getUUID(), p.getName().getString(), StringArgumentType.getString(ctx, "crystal"))))))
                        .then(literal("tier").then(argument("value", IntegerArgumentType.integer(1, 2))
                                .executes(ctx -> withPlayer(ctx, (admin, p) -> admin.setTier(actor(ctx),
                                        p.getUUID(), p.getName().getString(), IntegerArgumentType.getInteger(ctx, "value"))))))
                        .then(literal("energy").then(argument("value", IntegerArgumentType.integer())
                                .executes(ctx -> withPlayer(ctx, (admin, p) -> admin.setEnergy(actor(ctx),
                                        p.getUUID(), p.getName().getString(), IntegerArgumentType.getInteger(ctx, "value"))))))))
                .then(literal("energy")
                        .then(literal("audit").executes(ctx -> energyAudit(ctx, null))
                                .then(argument("player", EntityArgument.player())
                                        .executes(ctx -> energyAudit(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(literal("bottles").executes(this::bottles)))
                .then(literal("totem")
                        .then(literal("status").executes(this::totemStatus))
                        .then(literal("cancel").executes(this::totemCancel)))
                .then(literal("admin").executes(this::openAdmin))
                .then(literal("reload").executes(this::reload))
                .then(literal("event")
                        .then(literal("list").executes(ctx -> lines(ctx, admin().eventList())))
                        .then(literal("start").then(argument("id", StringArgumentType.word())
                                .suggests(suggest(AdminService::eventIds))
                                .executes(ctx -> reply(ctx, admin().eventStart(actor(ctx), StringArgumentType.getString(ctx, "id"))))))
                        .then(literal("stop").then(argument("id", StringArgumentType.word())
                                .suggests(suggest(AdminService::runningEventIds))
                                .executes(ctx -> reply(ctx, admin().eventStop(actor(ctx), StringArgumentType.getString(ctx, "id"))))))
                        .then(literal("simulate")
                                .then(literal("off").executes(ctx -> reply(ctx, admin().eventSimulateOff(actor(ctx)))))
                                .then(argument("date", StringArgumentType.word())
                                        .suggests(suggest(AdminService::simulateDateSuggestions))
                                        // "HH:mm" oder "HH:mm dry" – ':' ist in word() nicht erlaubt.
                                        .then(argument("time", StringArgumentType.greedyString())
                                                .suggests(suggest(admin -> List.of("18:00", "17:59 dry", "23:59 dry", "00:00")))
                                                .executes(this::simulate))))));
    }

    // ------------------------------------------------------------------ Befehle

    private int help(CommandContext<CommandSourceStack> ctx) {
        return lines(ctx, AdminService.help());
    }

    private int giveCrystal(CommandContext<CommandSourceStack> ctx, int tier, String stage) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String crystal = StringArgumentType.getString(ctx, "crystal");
        return reply(ctx, admin().giveCrystal(actor(ctx), target.getName().getString(), crystal, tier, stage,
                spec -> FabricItems.give(target, spec)));
    }

    private int giveItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String item = StringArgumentType.getString(ctx, "item");
        return reply(ctx, admin().giveItem(actor(ctx), target.getName().getString(), item,
                spec -> FabricItems.give(target, spec)));
    }

    private int reload(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ConfigManager.LoadResult result = admin().reload(actor(ctx));
        if (result.success()) {
            ctx.getSource().sendSuccess(() -> FabricText.legacy(Messages.prefixed("Neu geladen")), true);
        } else {
            ctx.getSource().sendFailure(FabricText.legacy(Messages.PREFIX
                    + "§cNeu laden fehlgeschlagen – vorherige Einstellungen bleiben aktiv:"));
        }
        result.messages().forEach(m -> ctx.getSource().sendSystemMessage(FabricText.legacy("§7· " + m)));
        return result.success() ? 1 : 0;
    }

    /** Die letzten 15 Energieänderungen (alle oder eines Spielers) aus {@code energy.log}. */
    private int energyAudit(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        List<EnergyLedger.Entry> entries = started().ledger().recent(target == null ? null : target.getUUID(), 15);
        if (entries.isEmpty()) {
            return lines(ctx, List.of(Messages.prefixed("Keine Einträge im Energie-Protokoll.")));
        }
        List<String> out = new ArrayList<>();
        out.add(Messages.prefixed("Energie-Protokoll" + (target == null ? "" : " von " + target.getName().getString())
                + " (neueste unten):"));
        for (EnergyLedger.Entry e : entries) {
            String time = e.time().length() >= 19 ? e.time().substring(0, 19).replace('T', ' ') : e.time();
            String color = EnergyLedger.SUSPICIOUS.equals(e.reason()) ? "§c" : "§f";
            out.add("§8" + time + " " + color + e.name() + " §7" + e.from() + " → " + e.to() + " §8(" + e.reason()
                    + (e.ref() != null ? ", " + e.ref() : "") + ")");
        }
        return lines(ctx, out);
    }

    private int bottles(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        BottleRegistry registry = started().bottles().registry();
        List<String> active = registry.activeIds();
        List<String> out = new ArrayList<>();
        out.add(Messages.prefixed(active.size() + " aktive Energy Bottles" + (active.size() > 15 ? " (die neuesten 15):" : ":")));
        for (String id : active.subList(Math.max(0, active.size() - 15), active.size())) {
            BottleRegistry.Entry entry = registry.entry(id);
            out.add("§8" + id.substring(0, 8) + " §7" + entry.origin + " · " + entry.creator + " · §8" + entry.created);
        }
        return lines(ctx, out);
    }

    private int totemStatus(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return lines(ctx, List.of(Messages.prefixed(started().totem().status().orElse("Kein Repair-Totem aktiv."))));
    }

    private int totemCancel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        XmasCore instance = started();
        if (!instance.totem().cancel(actor(ctx))) {
            return fail(ctx, "Kein Repair-Totem aktiv");
        }
        instance.audit().record(actor(ctx), "totem_cancel", null, null);
        return 1;
    }

    private int openAdmin(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        XmasCore instance = started();
        PlayerHandle handle = instance.platform().player(player.getUUID()).orElse(null);
        if (handle == null || !instance.adminGui().open(handle)) {
            return fail(ctx, "Admin-GUI konnte nicht geöffnet werden");
        }
        return 1;
    }

    private int simulate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String[] parts = StringArgumentType.getString(ctx, "time").trim().split("\\s+");
        boolean dry = parts.length > 1 && parts[1].equalsIgnoreCase("dry");
        if (parts.length > 2 || (parts.length == 2 && !dry)) {
            return fail(ctx, "Format: /xmas event simulate yyyy-MM-dd HH:mm [dry]");
        }
        return reply(ctx, admin().eventSimulate(actor(ctx), StringArgumentType.getString(ctx, "date"), parts[0], dry));
    }

    // ------------------------------------------------------------------ Hilfen

    @FunctionalInterface
    private interface PlayerAction {
        AdminService.Result run(AdminService admin, ServerPlayer player);
    }

    private int withPlayer(CommandContext<CommandSourceStack> ctx, PlayerAction action) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        return reply(ctx, action.run(admin(), player));
    }

    /** Verständliche Meldung statt "unerwarteter Fehler", wenn der Kern nicht läuft. */
    private AdminService admin() throws CommandSyntaxException {
        return started().admin();
    }

    private XmasCore started() throws CommandSyntaxException {
        XmasCore instance = core.get();
        if (instance == null) {
            throw NOT_STARTED.create();
        }
        return instance;
    }

    /** Spielername, sonst die Quelle (Befehlsblock, Funktion, RCON); die Server-Konsole als "Konsole". */
    private static String actor(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            return player.getName().getString();
        }
        String name = ctx.getSource().getTextName();
        return name == null || name.isBlank() || name.equals("Server") ? AdminService.CONSOLE : name;
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, AdminService.Result result) {
        if (result.success()) {
            ctx.getSource().sendSuccess(() -> FabricText.legacy(Messages.prefixed(result.message())), true);
            return 1;
        }
        return fail(ctx, result.message());
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendFailure(FabricText.legacy(Messages.PREFIX + "§c" + message));
        return 0;
    }

    private static int lines(CommandContext<CommandSourceStack> ctx, List<String> lines) {
        lines.forEach(line -> ctx.getSource().sendSystemMessage(FabricText.legacy(line)));
        return 1;
    }

    private SuggestionProvider<CommandSourceStack> suggest(Function<AdminService, List<String>> values) {
        return (ctx, builder) -> {
            XmasCore instance = core.get();
            return instance == null ? builder.buildFuture()
                    : SharedSuggestionProvider.suggest(values.apply(instance.admin()), builder);
        };
    }
}
