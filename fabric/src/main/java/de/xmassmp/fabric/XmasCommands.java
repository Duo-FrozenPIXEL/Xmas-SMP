package de.xmassmp.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import de.xmassmp.XmasCore;
import de.xmassmp.admin.AdminService;
import de.xmassmp.text.Messages;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

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

    private int reload(CommandContext<CommandSourceStack> ctx) {
        List<String> messages = admin().reload(actor(ctx));
        ctx.getSource().sendSuccess(() -> FabricText.legacy(Messages.prefixed("Neu geladen")), true);
        messages.forEach(m -> ctx.getSource().sendSystemMessage(FabricText.legacy("§7· " + m)));
        return 1;
    }

    private int simulate(CommandContext<CommandSourceStack> ctx) {
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

    private AdminService admin() {
        XmasCore instance = core.get();
        if (instance == null) {
            throw new IllegalStateException("Xmas-SMP ist noch nicht gestartet");
        }
        return instance.admin();
    }

    private static String actor(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        return player != null ? player.getName().getString() : AdminService.CONSOLE;
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
